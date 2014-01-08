class apache_balancer::balancer_install2()
{
  
class {'apache_balancer_deploy':}

class {'generate_cert':
cwd => '/usr/local/apache2/ssl/'
  }

class {'apache_start':}

utils::add_line {'worker1':
   name =>'worker1',
path_to_file =>"/etc/hosts",
text => "192.168.57.101  dev01.hortonworks.com worker1"
}
utils::add_line {'worker2':
 name =>'worker2',
path_to_file =>"/etc/hosts",
text => "192.168.56.102 dev01.hortonworks.com worker2"
}


}