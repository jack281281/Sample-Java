#!/usr/bin/groovy

def call(String message = 'SUCCESS') {
	//office365ConnectorSend message: "Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})", status: message, webhookUrl: "${env.TEAMS_CHANNEL_BUILDS_CONNECTOR_365_URL}"
}