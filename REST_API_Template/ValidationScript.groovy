import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def flowFile = session.get()
if (flowFile) {
    def requestBody = flowFile.read().getText('UTF-8')
    log.info("Request Body: ${requestBody}")

    if (requestBody) {
        try {
            def jsonSlurper = new JsonSlurper()
            def json = jsonSlurper.parseText(requestBody)
            log.info("Parsed JSON: ${json}")

            // Create a list to hold the names of missing parameters
            List<String> missingParams = []

            // Check for the required parameters and add to the missingParams list if missing
            if (!json.url) missingParams.add("url")
            if (!json.env) missingParams.add("env")
            if (!json.service) missingParams.add("service")

            if (missingParams.isEmpty()) {
                flowFile = session.putAttribute(flowFile, 'route.to', 'success')
            } else {
                flowFile = session.putAttribute(flowFile, 'route.to', 'error')
                // Create a JSON object with the "missing params" key and the missingParams list
                def jsonResponse = JsonOutput.toJson(["missing params": missingParams])
                flowFile = session.putAttribute(flowFile, 'missing.params', jsonResponse)
            }
        } catch (Exception e) {
            flowFile = session.putAttribute(flowFile, 'route.to', 'error')
            log.error("Error occurred while processing the script. Setting 'route.to' to 'error'.", e)
        }
    } else {
        flowFile = session.putAttribute(flowFile, 'route.to', 'error')
        log.error("Request body is empty or null. Setting 'route.to' to 'error'.")
    }
}
session.transfer(flowFile, REL_SUCCESS)
