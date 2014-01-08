class apache_balancer::balancer_install()
{

#openssl::certificate::x509 { 'server':
#  ensure       => present,
#  country      => 'UA',
#  organization => 'hortonworks.com',
#  state        => 'Here',
#  commonname   => $fqdn,
#  locality     => 'Kiev',
#  unit         => 'MyUnit',
#  altnames     => ['a.com', 'b.com', 'c.com'],
#  email        => 'contact@server.com',
#  days         => 3456,
 # base_dir     => '/var/www/ssl',
 # owner        => 'www-data',
  #password     => 'j(D$',
#  force        => false,
#  cnf_tpl      => 'my_module/cert.cnf.erb'
#}


class { 'apache':
 default_mods => 'false'
  }

 apache::vhost { 'ssl.example.com':
      port    => '443',
      docroot => '/var/www/ssl',
      ssl     => true,
    }

 apache::balancer
 { 'apache-balancer':
   proxy_set => { failontimeout => 'On', lbmethod => 'byrequests', stickysession => 'ROUTEID'}
 }
 
 apache::balancermember { "worker1-apache-balancer":
  balancer_cluster => 'apache-balancer',
  url              => "https://worker1.hortonworks.com:8443",
#  options          => ['ping=5', 'disablereuse=on', 'retry=5', 'ttl=120'],
      }

 apache::balancermember { "worker2-apache-balancer":
  balancer_cluster => 'apache-balancer',
  url              => "https://worker2.hortonworks.com:8444",
#  options          => ['ping=5', 'disablereuse=on', 'retry=5', 'ttl=120'],
      }
 
#utils::load_module {'lbmethod_byrequests':
#modulename =>'lbmethod_byrequests'
#}
utils::load_module {'worker1':
path_to_file => "/etc/hosts",
string => "192.168.56.101  worker1.hortonworks.com worker02"
}

include apache::mod::proxy
include apache::mod::proxy_http
include apache::mod::ssl
#apache::mod {'lbmethod_byrequests':}
 
package { "make":
    ensure => "installed"
}
#utils::write_line {'worker1':
#path_to_file =>"/etc/hosts",
#text => "192.168.56.101  worker1.hortonworks.com worker01"
#}
#utils::write_line {'worker2':
#path_to_file =>"/etc/hosts",
#text => "192.168.56.102 worker2.hortonworks.com worker02"
#}
}
