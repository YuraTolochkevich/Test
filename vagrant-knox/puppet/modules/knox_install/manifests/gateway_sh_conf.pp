class knox_install::gateway_sh_conf ()
{
        config_sh{ 'knox1': knox_folder=> "knox1", conf_path => "/knox1/bin/gateway.sh"}
        config_sh{  'knox2': knox_folder=> "knox2", conf_path => "/knox2/bin/gateway.sh"}
        require   knox_install::knox_download
}

define config_sh($knox_folder,$conf_path)
{
        file
        { $knox_folder:
                path    => $conf_path,
                owner   => root,
                group   => root,
                mode    => 777,
                content => template("knox_install/gateway.sh.erb"),

        }

        
}