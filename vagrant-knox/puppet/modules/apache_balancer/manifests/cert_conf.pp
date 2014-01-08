define cert_conf($conf_path)
{
        file
        { $conf_path:
                path    => $conf_path,
                owner   => root,
                group   => root,
                mode    => 777,
                content => template("apache_balancer/cert.cfg.erb"),

        }

        
}