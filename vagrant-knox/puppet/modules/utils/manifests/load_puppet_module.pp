define utils::load_puppet_module 
(
$site = "",
$file_name = "",
$cwd ="",
) {
package { "zip":
    ensure => "installed"
}
exec { 'load':
#cwd => $cwd,
command => "wget ${site}/${file_name}", 
 path => "/usr/local/bin:/bin:/usr/bin",
#command => "unzip ${name} -d ${name}"
}
exec {'unzip':
cwd => $cwd,
command => "unzip ${file_name} ",
path => "/usr/local/bin:/bin:/usr/bin"
}
}
