package flank.corellium.domain.test.android.task

import flank.corellium.api.AndroidTestPlan
import flank.corellium.domain.TestAndroid
import flank.corellium.domain.TestAndroid.Authorize
import flank.corellium.domain.TestAndroid.ExecuteTests
import flank.corellium.domain.TestAndroid.InstallApks
import flank.corellium.domain.TestAndroid.InvokeDevices
import flank.corellium.domain.TestAndroid.ParseApkInfo
import flank.corellium.domain.TestAndroid.PrepareShards
import flank.corellium.domain.TestAndroid.context
import flank.exection.parallel.from
import flank.exection.parallel.using
import flank.instrument.command.formatAmInstrumentCommand
import flank.instrument.log.Instrument
import flank.instrument.log.parseAdbInstrumentLog
import flank.shard.Shard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import java.io.File

/**
 * Executes given tests on previously invoked devices, and returns the test results.
 *
 * The side effect is console logs from `am instrument` saved inside [TestAndroid.ExecuteTests.ADB_LOG] output subdirectory.
 *
 * The optional parsing errors are sent through [TestAndroid.Context.out].
 */
internal val executeTests = ExecuteTests from setOf(
    PrepareShards,
    ParseApkInfo,
    Authorize,
    InvokeDevices,
    InstallApks,
) using context {
    val outputDir = File(args.outputDir, ExecuteTests.ADB_LOG).apply { mkdir() }
    val testPlan: AndroidTestPlan.Config = prepareTestPlan()
    coroutineScope {
        ExecuteTests.Plan(testPlan).out()
        api.executeTest(testPlan).map { (id, flow) ->
            async {
                var read = 0
                var parsed = 0
                val file = outputDir.resolve(id)
                val results = mutableListOf<Instrument>()
                flow.onEach { file.appendText(it + "\n") }
                    .buffer(100)
                    .flowOn(Dispatchers.IO)
                    .onEach { ++read }
                    .parseAdbInstrumentLog()
                    .onEach { parsed = read }
                    .onEach { result -> results += result }
                    .onEach { result -> ExecuteTests.Result(id, result).out() }
                    .catch { cause -> ExecuteTests.Error(id, cause, file.path, ++parsed..read).out() }
                    .filterIsInstance<Instrument.Result>()
                    .take(expectedResultsCountFor(id))
                    .collect()
                results
            }
        }.awaitAll()
    }
}

/**
 * Prepare [AndroidTestPlan.Config] for test execution.
 * It is just mapping and formatting the data collected in state.
 */
private fun TestAndroid.Context.prepareTestPlan(): AndroidTestPlan.Config =
    AndroidTestPlan.Config(
        shards.mapIndexed { index, shards ->
            ids[index] to shards.flatMap { shard: Shard.App ->
                shard.tests.map { test ->
                    formatAmInstrumentCommand(
                        packageName = packageNames.getValue(test.name),
                        testRunner = testRunners.getValue(test.name),
                        testCases = test.cases.map { case -> "class " + case.name }
                    )
                }
            }
        }.toMap()
    )

private fun TestAndroid.Context.expectedResultsCountFor(id: String): Int =
    shards[ids.indexOf(id)].size