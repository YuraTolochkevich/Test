class knox_install::knox_start(
  $password ="123456"
)
{
  require knox_install::gateway_conf
  require knox_install::gateway_sh_conf
  require knox_install::sandbox_conf
  
  exec { 'start_ldap': 
 cwd => "/knox1",
 path => "/usr/local/bin:/bin:/usr/bin",
command => "java -jar bin/ldap.jar conf &",
 }
   exec { 'iptables_stop':
  path => "/usr/local/bin:/bin:/usr/bin",
command => "/etc/init.d/iptables stop",
 }
 
  
   exec { 'setup_knox1': 
 cwd => "/knox1/bin",
 path => "/usr/local/bin:/bin:/usr/bin",
command => "sh gateway.sh setup ",
require => Exec["start_ldap"],
 }
    exec { 'setup_knox2': 
 cwd => "/knox2/bin",
 path => "/usr/local/bin:/bin:/usr/bin",
command => "sh gateway.sh setup",
require => Exec["start_ldap"],
 }
 
 utils::add_line {'enable_debug_knox1':
   name =>'knox1_debug',
path_to_file =>"/knox1/conf/log4j.properties",
text => "log4j.logger.org.apache.hadoop.gateway=DEBUG"
}
 utils::add_line {'enable_debug_knox2':
   name =>'knox2_debug',
path_to_file =>"/knox2/conf/log4j.properties",
text => "log4j.logger.org.apache.hadoop.gateway=DEBUG"
}
 
 utils::add_line {'add_dev01_to_hosts':
   name =>'add_dev01',
path_to_file =>"/etc/hosts",
text => "192.168.57.101 dev01.hortonworks.com dev01"
} 
 
utils::add_line {'add_dev02_to_hosts':
   name =>'add_dev02',
path_to_file =>"/etc/hosts",
text => "192.168.57.102 dev02.hortonworks.com dev02"
} 

utils::add_line {'add_sandbox_to_hosts':
   name =>'add_sandbox',
path_to_file =>"/etc/hosts",
text => "192.168.57.103   sandbox.hortonworks.com  sandbox"
}  
 
}