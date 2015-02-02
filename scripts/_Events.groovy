import grails.util.Environment
eventCompileEnd = {x ->
    if(Environment.currentEnvironment!=Environment.TEST) {
        compileSass()
    }
}

def compileSass() {
    def baseDir = System.getProperty("base.dir")
    def command
    if(baseDir != null) {
        command = """${baseDir}/scripts/compass-compile compact ${baseDir}"""// Create the String
    } else {
        command = """scripts/compass-compile compact ."""// Create the String
    }
    def proc = command.execute()                 // Call *execute* on the string
    proc.waitFor()                               // Wait for the command to finish

    if(proc.exitValue() == 0) {
        def messages = proc.in.text.split("\n")
        messages.each { message ->
            event("StatusUpdate",[message])
        }
    } else {
        event("StatusError", ["Problem compiling SASS ${proc.err.text}"])
    }
}
