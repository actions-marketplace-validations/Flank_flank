package ftl.run.platform.android

import ftl.api.RemoteStorage
import ftl.api.uploadToRemoteStorage
import ftl.args.AndroidArgs
import ftl.args.IArgs
import ftl.config.FtlConstants.GCS_PREFIX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.nio.file.Files
import java.nio.file.Paths

internal suspend fun IArgs.uploadOtherFiles(): Map<String, String> = coroutineScope {
    otherFiles
        .map { (devicePath: String, filePath: String) ->
            async(Dispatchers.IO) { devicePath to upload(filePath, resultsBucket, resultsDir) }
        }.awaitAll().toMap()
}

internal suspend fun AndroidArgs.uploadObbFiles(): Map<String, String> = coroutineScope {
    obbFiles.map {
        async(Dispatchers.IO) { it to upload(it, resultsBucket, resultsDir) }
    }.awaitAll().toMap()
}

fun upload(file: String, rootGcsBucket: String, runGcsPath: String): String =
    if (file.startsWith(GCS_PREFIX)) file
    else uploadToRemoteStorage(
        RemoteStorage.Dir(rootGcsBucket, runGcsPath),
        RemoteStorage.Data(file, Files.readAllBytes(Paths.get(file)))
    )
