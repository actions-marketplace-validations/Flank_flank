package ftl.presentation.cli

import ftl.presentation.cli.firebase.CancelCommand
import ftl.presentation.cli.firebase.TestCommand
import ftl.presentation.cli.firebase.test.refresh.RefreshCommand
import ftl.util.PrintHelpCommand
import picocli.CommandLine.Command

@Command(
    name = "firebase",
    synopsisHeading = "",
    subcommands = [
        TestCommand::class,
        RefreshCommand::class,
        CancelCommand::class
    ],
    usageHelpAutoWidth = true
)
class FirebaseCommand : PrintHelpCommand()
