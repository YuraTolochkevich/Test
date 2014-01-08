class apache_balancer::install_openssl 
(
  $ssl_path="/usr/local/apache2/ssl",
){
  require apache_balancer::apache_balancer_deploy
  package { 'openssl-devel':
    ensure => present,
  }
  file { $ssl_path:
    ensure => "directory",
}
  }

