package com.canerector.builds.msbuild

abstract class BuildModuleBase  implements Serializable {
	
	def pipeline
	
	BuildModuleBase(pipeline){ this.pipeline = pipeline }
	
	def projectName
	def projectBranchName
	def gitHubUrl
	def slackFormattedGitHubUrl
	def slackFormattedBuildUrl
	
	def performBuild(){
	
		projectName = pipeline.env.JOB_NAME.replace('canerectors/', '').replace('/' + pipeline.env.BRANCH_NAME, '')
		projectBranchName = projectName + ':' + pipeline.env.BRANCH_NAME
		gitHubUrl = pipeline.github.getProjectUrl(projectName, pipeline.env.BRANCH_NAME)
		slackFormattedGitHubUrl = pipeline.slack.getMessageStringForUrl(gitHubUrl, projectBranchName)
		slackFormattedBuildUrl = pipeline.slack.getMessageStringForUrl(pipeline.env.BUILD_URL, 'Build #' + pipeline.env.BUILD_NUMBER)
		
		pipeline.timestamps{
			try{
				sendSlackMessage('started for project: ' + slackFormattedGitHubUrl)		
	
				performBuildInternal()
			}
			catch(err){
		
				print err
		
				def consoleUrl = pipeline.slack.getMessageStringForUrl(pipeline.env.BUILD_URL + 'console', 'Build Log.')		
		
				sendSlackMessage('for project: ' + slackFormattedGitHubUrl + ' failed. See ' + consoleUrl, 'danger')		
		
				currentBuild.result = 'FAILURE'
			}
		}
	}
	
	abstract def performBuildInternal()
	
	def checkout(){
	
		pipeline.stage('Checkout') {
			pipeline.checkout pipeline.scm
			
			pipeline.bat 'git submodule update --init --recursive'								
			
			pipeline.bat 'git checkout %BRANCH_NAME% && git pull'
			pipeline.bat 'git remote remove origin1'  //this is for gitversion. it can't handle more than one remote
		}
	}

	def nugetRestore(){
		pipeline.stage('Nuget Restore') {
			pipeline.nuget.restore()
		}
	}

	def sendSlackMessage(message, color = 'good', channel = '#builds'){
		pipeline.node{
			pipeline.slack.sendMessage(slackFormattedBuildUrl + ' ' + message, color, channel)
		}
	}
}