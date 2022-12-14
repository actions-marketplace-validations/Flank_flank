package ftl.api

import ftl.adapter.GcFetchArtifacts

val fetchArtifacts: Artifacts.Fetch get() = GcFetchArtifacts

object Artifacts {

    data class Identity(
        val gcsPathWithoutRootBucket: String,
        val gcsRootBucket: String,
        val regex: List<Regex>,
        val downloadPath: DownloadPath,
        val matrixId: String
    )

    data class DownloadPath(
        val localResultDir: String,
        val useLocalResultDir: Boolean,
        val keepFilePath: Boolean
    )

    /**
     * Fetches all files for provided [Identity].
     * @return Matrix ID of which files were downloaded
     * associated with list of downloaded files
     */
    interface Fetch : (Identity) -> Pair<String, List<String>>
}
