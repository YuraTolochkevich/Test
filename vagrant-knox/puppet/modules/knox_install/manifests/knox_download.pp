class knox_install::knox_download (
  $cwd = "/",
  $url = "",
  $file_name="",
  
) {
  
   
  class { 'java':
  distribution => 'jdk',
  version      => 'latest',

}

  
  package { 'git':
    ensure => installed,
  }

 class { "maven::maven": } 
 
 
 exec { 'load_knox':
 cwd => $cwd,
 command => "wget ${url}/${file_name}.zip", 
 path => "/usr/local/bin:/bin:/usr/bin",
 require =>Class["java"],
 }


 exec { 'install': 
 cwd => $cwd,
 path => "/usr/local/bin:/bin:/usr/bin",
command => "jar xf  ${file_name}.zip",
require => Exec["load_knox"],
}
exec {'create_knox1':
 cwd => $cwd,
 path => "/usr/local/bin:/bin:/usr/bin",
command => "cp -r /${file_name} /knox1",

require => Exec["install"],
}
  
exec {'create_knox2':
 cwd => $cwd,
 path => "/usr/local/bin:/bin:/usr/bin",
command => "cp -r /${file_name} /knox2",
 require => Exec["install"],
}
exec {'set_permissions':
 cwd => $cwd,
 path => "/usr/local/bin:/bin:/usr/bin",
command => "chmod -R 777  /knox2/ /knox1/ ",
 
 require => Exec["create_knox2","create_knox1"],
}


}