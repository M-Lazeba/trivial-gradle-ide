package org.example

import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.events.ProgressEvent
import org.gradle.tooling.events.ProgressListener
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.events.OperationType
import org.gradle.tooling.events.task.TaskOperationDescriptor
import org.gradle.tooling.events.task.TaskProgressEvent
import java.nio.file.Path
import kotlin.math.max


fun main() {
  val workdir = "/path/to/test/gradle/project"
  val task = ":runAllTasks"

  runBuild(workdir, task)

}

var firstTaskTs: Long = 0L
var firstTaskProcessedTs: Long = 0L
var lastTaskTs: Long = 0L
var lastTaskProcessedTs: Long = 0L
val descriptorTreeSizes = mutableListOf<TreeInfo>()

fun runBuild(workdir: String, task: String) {
  try {
    createGradleConnection(workdir).use { connection ->
      // Load the project
      val buildLauncher: BuildLauncher = connection.newBuild()
      buildLauncher.setStandardOutput(System.out)
      buildLauncher.setStandardError(System.out)

      buildLauncher.addProgressListener(TasksListener(), OperationType.TASK)
      // Configure the task to be executed
      val launcher: BuildLauncher = buildLauncher.forTasks(task)
      // Execute the task
      launcher.run()
    }
  } catch (e: GradleConnectionException) {
    println("Error connecting to Gradle.")
    e.printStackTrace()
  } catch (e: Exception) {
    println("Error executing Gradle task.")
    e.printStackTrace()
  }
  println("Tasks execution time: ${lastTaskTs - firstTaskTs}")
  println("First event lag: ${firstTaskProcessedTs - firstTaskTs}")
  println("Last event lag: ${lastTaskProcessedTs - lastTaskTs}")
  println("Events number: ${descriptorTreeSizes.size}")
  println("Tree sizes, ${descriptorTreeSizes.map { it.size }.printAvgAndMax()}")
  println("Tree heights, ${descriptorTreeSizes.map { it.height }.printAvgAndMax()}")
}

private fun Iterable<Int>.printAvgAndMax() = "avg: ${average()}, max: ${max()}"

private fun createGradleConnection(workdir: String): ProjectConnection {
  // Get current working directory
  val projectPath: Path = Path.of(workdir)
  val projectDir = projectPath.toFile()

  // Initialize the Tooling API
  return GradleConnector
    .newConnector()
//    .useInstallation(File("/path/to/local/gradle/local_build_installation/"))
    .forProjectDirectory(projectDir)
    .connect()
}

class TasksListener : ProgressListener {
  override fun statusChanged(p: ProgressEvent) {
    if (firstTaskTs == 0L) {
      firstTaskTs = p.eventTime
      firstTaskProcessedTs = System.currentTimeMillis()
    }
    lastTaskTs = p.eventTime
    lastTaskProcessedTs = System.currentTimeMillis()


    if (p is TaskProgressEvent) {
      println("Start evaluating tree for ${p.displayName}")
      val visitedSet = HashSet<String>()
      val h = evaluateDescriptorDependencies(p.descriptor, visitedSet, 1)
      println("Finish evaluating tree for ${p.displayName}")
      descriptorTreeSizes.add(TreeInfo(visitedSet.size + 1, h))
    }
  }

  fun evaluateDescriptorDependencies(d: TaskOperationDescriptor, visited: MutableSet<String>, level: Int): Int {
    var height = level
    d.dependencies.forEach {
      if (visited.contains(it.name)) return@forEach
      visited.add(it.name)

      if (it is TaskOperationDescriptor) {
        val subHeight = evaluateDescriptorDependencies(it, visited, level + 1)
        height = max(subHeight, height)
      }
    }
    return height
  }

}


class TreeInfo(val size: Int, val height: Int)
