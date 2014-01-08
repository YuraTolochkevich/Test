class apache_balancer::install_apr ()
{
  utils::install { 'apr':
  download => 'apache.volia.net//apr/apr-1.5.0.tar.gz',
  creates  => '/usr/local/bin/apr',
}

}