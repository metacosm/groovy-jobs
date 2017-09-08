/* v1.1.0
 * Version tested with volume mounted and jenkins-jnlp-maven-prod docker image
 * Add command otherwise it fails with a previous version of kubernetes Jenkins Plugin
 */
def imagePath = OPENSHIFT_DOCKER_REGISTRY_IP_PORT + "/" + OPENSHIFT_PROJECT + "/jenkins-jnlp-maven-prod"

withCredentials([usernamePassword(credentialsId: 'cmoullia', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {

    podTemplate(
            cloud: 'openshift',
            label: 'release-pod',
            name: 'compile-java-tools',
            namespace: OPENSHIFT_PROJECT,
            containers: [
                    containerTemplate(
                            name: 'jnlp',
                            image: imagePath,
                            workingDir: '/tmp',
                            alwaysPullImage: true,
                            serviceAccount: 'jenkins',
                            args: '${computer.jnlpmac} ${computer.name}',
                            command: 'run-jnlp-client',
                            envVars: [
                                    containerEnvVar(key: 'JENKINS_HOST', value: JENKINS_HOST),
                                    containerEnvVar(key: 'JENKINS_PORT', value: JENKINS_PORT),
                                    containerEnvVar(key: 'KERBEROS_USER', value: USER),
                                    containerEnvVar(key: 'KERBEROS_PWD', value: PASSWORD),
                                    containerEnvVar(key: 'JENKINS_LDAP', value: JENKINS_LDAP)
                            ]
                    )
            ], volumes: [
            persistentVolumeClaim(
                    mountPath: '/tmp',
                    claimName: 'productization-repo',
                    readOnly: false)
    ]) {

        node('release-pod') {
            // Clone licenses project and built it
            stage('Clone and build licenses generator') {
                git credentialsId: 'cmoullia', branch: 'master', poll: false, url: GIT_REPO_LIC
                sh """
          mvn -B install -DskipTests=true -Dmaven.repo.local=/tmp/workspace/.m2/repository
        """
            }

            // Clone project and build it
            stage('Clone and build product-files-generator') {
                git credentialsId: 'cmoullia', branch: 'spring-boot', poll: false, url: GIT_REPO_PROD
                sh """
          mvn -B package -DskipTests=true -Dmaven.repo.local=/tmp/workspace/.m2/repository
        """
            }
        }
    }
}