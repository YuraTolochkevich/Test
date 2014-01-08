class knox_install::gateway_conf ()
{
        config{ 'knox1': listenport=> "8443", conf_path => "/knox1/conf/gateway-site.xml"}
        config{  'knox2': listenport=> "8444", conf_path => "/knox2/conf/gateway-site.xml"}
        require   knox_install::knox_download
   
}

define config($listenport,$conf_path)
{
        file
        { $conf_path:
                path    => $conf_path,
                owner   => root,
                group   => root,
                mode    => 777,
                content => template("knox_install/gateway-site.xml.erb"),

        }

        
}