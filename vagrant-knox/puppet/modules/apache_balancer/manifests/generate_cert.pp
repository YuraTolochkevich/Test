class apache_balancer::generate_cert (
  $cwd = ''
)
{
  require apache_balancer::install_openssl
  
  cert_conf {'cert': 
     conf_path => "${cwd}/cert.cfg"
     } ->
  
  
  exec { 'gen_pk': 
 cwd => $cwd,
 path => "/usr/local/bin:/bin:/usr/bin",
command => "openssl genrsa -des3 -out server.key  -passout pass:123456 1024",
#require  => cert_conf['cert']
}
  exec { 'gen_csr':
 cwd => $cwd,
 path => "/usr/local/bin:/bin:/usr/bin",
command => "openssl req -new -key server.key -out server.csr  -config cert.cfg -passin pass:123456",
require => Exec["gen_pk"],

}
  exec { 'cp_pass':
 cwd => $cwd,
 path => "/usr/local/bin:/bin:/usr/bin",
command => "cp -f server.key server.key.org",
require => Exec["gen_csr"],
}
  exec { 'remove_passphrase':
 cwd => $cwd,
 path => "/usr/local/bin:/bin:/usr/bin",
command => "openssl rsa -in server.key.org -out server.key -passin pass:123456",
require => Exec["cp_pass"],
}
  exec { 'gen_cert':
 cwd => $cwd,
 path => "/usr/local/bin:/bin:/usr/bin",
command => "openssl x509 -req -days 365 -in server.csr -signkey server.key -out server.crt",
require => Exec["cp_pass"],
}
  exec { 'cp_cert':
 cwd => $cwd,
 path => "/usr/local/bin:/bin:/usr/bin",
command => "cp server.crt ssl.crt",
require => Exec["gen_cert"],
}
  exec { 'cp_ket':
 cwd => $cwd,
 path => "/usr/local/bin:/bin:/usr/bin",
command => "cp server.key ssl.key",
require => Exec["gen_cert"],
}
}