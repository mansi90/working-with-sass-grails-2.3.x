import grails.util.Environment
eventCreateWarStart = {warname, stagingDir ->

    //println("---------- BuildAppVersionPropertiesStart ----------")
    event("BuildAppVersionPropertiesStart", [warname, stagingDir])

    Ant.propertyfile(file: "${stagingDir}/WEB-INF/classes/application.properties") {
		Ant.antProject.properties.findAll({k,v-> k.startsWith('environment')}).each { k,v->
            entry(key: k, value: v)
		}
        entry(key: 'scm.revision', value: getRevision().trim())
        entry(key: 'scm.branch', value: getBranch().trim())
        entry(key: 'buildtime', value: new Date())
        entry(key: 'buildhost', value: java.net.InetAddress.getLocalHost().getHostName())
    }

    event("BuildAppVersionPropertiesEnd", [warname, stagingDir])
}

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

def getBranch() {

    // try to get branch from Jenkins
    def scmBranch = Ant.antProject.properties."environment.GIT_BRANCH"

    // try git locally
    if (!scmBranch) {
        try {
            def command = """git rev-parse --abbrev-ref HEAD"""
            def proc = command.execute()
            proc.waitFor()
            if (proc.exitValue() == 0) {
                scmBranch = proc.in.text
            }
        } catch (IOException e) {
            throw new RuntimeException(e)
        }
    }

    return scmBranch ?: 'UNKNOWN'
}

def getRevision() {

    // try to get revision from Jenkins
    def scmVersion = Ant.antProject.properties."environment.GIT_COMMIT"

    // try git locally
    if (!scmVersion) {
        try {
            def command = """git rev-parse HEAD"""
            def proc = command.execute()
            proc.waitFor()
            if (proc.exitValue() == 0) {
                scmVersion = proc.in.text
            }
        } catch (IOException e) {
            throw new RuntimeException(e)
        }
    }

    return scmVersion ?: 'UNKNOWN'
}
