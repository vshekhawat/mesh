node('dockerSlave') {
   stage 'Checkout'
   checkout scm
   def mvnHome = tool 'M3'

   stage 'VersionSet'
   def v = version()
   if (v) {
     echo "Building version ${v}"
   }
   sh "${mvnHome}/bin/mvn versions:set -DnewVersion=${env.BUILD_NUMBER}"

   stage 'Build'
   sshagent(['601b6ce9-37f7-439a-ac0b-8e368947d98d']) {
     sh "${mvnHome}/bin/mvn -B -Dmaven.test.failure.ignore clean install scm:tag"
     step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
   }
}

def version() {
  def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
  matcher ? matcher[0][1] : null
}
