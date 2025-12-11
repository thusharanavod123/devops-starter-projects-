pipeline {
    // 1. Specify that this pipeline can run on any available Jenkins agent
    agent any

    // Add a tools block to make the Docker command-line available
    tools {
        docker 'docker'
    }

    // 2. Define environment variables used throughout the pipeline
    environment {
        // Use the Jenkins build number to create a unique tag for each image
        IMAGE_TAG = "devops-starter/nginx-app:${env.BUILD_NUMBER}"
    }

    // 3. Define the stages of our pipeline
    stages {
        stage('Checkout Code') {
            steps {
                // This step checks out the source code from your Git repository
                echo 'Checking out source code...'
                checkout scm
            }
        }

        stage('Build Docker Image') {
            steps {
                // This stage uses the Docker plugin to build the image
                echo "Building Docker image: ${IMAGE_TAG}"
                script {
                    docker.build(IMAGE_TAG, '.')
                }
            }
        }

        stage('Test Run Container') {
            steps {
                // Run the container in the background to verify it starts
                echo "Running container from image ${IMAGE_TAG} for a quick test..."
                // Use a script block for multi-line shell commands
                script {
                    sh "docker run --name nginx-test-${env.BUILD_NUMBER} -d -p 8080:80 ${IMAGE_TAG}"
                    sleep 5 // Wait for Nginx to start
                    echo "Testing web server response..."
                    sh "curl -f http://localhost:8080" // -f fails the build on HTTP errors
                }
            }
        }
    }

    // 4. Define post-build actions that will always run
    post {
        always {
            // This is a crucial cleanup step to remove the test container
            echo "Cleaning up test container..."
            sh "docker stop nginx-test-${env.BUILD_NUMBER} || true"
            sh "docker rm nginx-test-${env.BUILD_NUMBER} || true"
        }
    }
}