package flank.scripts.cli.shell.buildexample

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import flank.scripts.ops.shell.buildexample.ios.buildTestPlansExample

object BuildTestPlansExample : CliktCommand(
    name = "build_ios_testplans_example",
    help = "Build ios test plans example app"
) {
    private val generate: Boolean? by option(help = "Make build")
        .flag("-g", default = true)

    private val copy: Boolean? by option(help = "Copy output files to tmp")
        .flag("-c", default = true)

    override fun run() {
        buildTestPlansExample(generate, copy)
    }
}