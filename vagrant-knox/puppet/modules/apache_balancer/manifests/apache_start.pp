class apache_balancer::apache_start (
  $path='/usr/local/apache2/bin/'
)
{
  require apache_balancer::generate_cert
  exec { 'apache_start':
 cwd => $path,
 path => "/usr/local/bin:/bin:/usr/bin",
command => "${path}/apachectl -k start",
}

  exec { 'iptables_stop':
 cwd => $path,
 path => "/usr/local/bin:/bin:/usr/bin",
command => "/etc/init.d/iptables stop",

}
}
