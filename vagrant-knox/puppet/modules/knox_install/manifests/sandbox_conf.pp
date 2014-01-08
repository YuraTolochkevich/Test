class knox_install::sandbox_conf ()
{
        config_sbox{ 'knox1':  conf_path => "/knox1/deployments/sandbox.xml"}
        config_sbox{ 'knox2':  conf_path => "/knox2/deployments/sandbox.xml"}
require   knox_install::knox_download
}

define config_sbox($conf_path)
{
        file
        { $conf_path:
                path    => $conf_path,
                owner   => root,
                group   => root,
                mode    => 777,
                content => template("knox_install/sandbox.xml.erb"),

        }

        
}