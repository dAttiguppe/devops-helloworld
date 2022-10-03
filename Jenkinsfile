node{
    stage('SCM Checkout'){
       git 'https://github.com/samirjarika/valaxytech-helloworld.git'
    }
    stage('Compile-Package'){
      sh 'mvn package' 
    }
}
