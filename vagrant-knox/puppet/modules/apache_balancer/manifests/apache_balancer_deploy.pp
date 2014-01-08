class apache_balancer::apache_balancer_deploy()
{
  
utils::install { 'apr':
  download => 'apache.volia.net//apr/apr-1.5.0.tar.gz',
  creates  => '/usr/local/bin/apr',
} ->

utils::install { 'apr-utils':
 
  download => 'apache.ip-connect.vn.ua//apr/apr-util-1.5.3.tar.gz',
  creates  => '/usr/local/bin/apr-utils',
  buildoptions =>'--with-apr=/usr/local/apr',
 # require => utils::install['apr'], 
} ->

utils::install { 'pcre':
 
  download => 'ftp://ftp.csx.cam.ac.uk/pub/software/programming/pcre/pcre-8.33.tar.gz',
  creates  => '/usr/local/bin/pcre',
 # require => utils::install['apr-utils'],
  
} ->

utils::install { 'apache':
 
  download => 'apache.ip-connect.vn.ua//httpd/httpd-2.4.7.tar.gz',
  creates  => '/usr/local/bin/apache2',
  buildoptions => '-with-pcre=/usr/local/bin/pcre-config --enable-ssl',
#require => utils::install['pcre'],
} ->
class {'apache_conf':
#require => utils::install['apache']
}


}