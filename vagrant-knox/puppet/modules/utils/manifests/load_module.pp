define utils::load_module  
(
$string='',
$path_to_file=''
)
{
#file_line { 'f':
#path => $path_to_file,
#line => $string 
#   path => '/etc/apache2/apache2.conf',
#	line => "LoadModule ${modulename}_module modules/mod_${modulename}.so",
#}
}
