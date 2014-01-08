define utils::add_line
(
$text= '',
$path_to_file  = '',
$name = ''
)
{
file_line {  $name:
   path => $path_to_file,
   line => $text,
}
}
