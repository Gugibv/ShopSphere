pipeline {
    agent any

    triggers {
        githubPush()
    }


    stages {
        stage('Triggered') {
            steps {
                echo 'Jenkins CI/CD 已被 GitHub Push 成功触发!'
            }
        }
    }
}