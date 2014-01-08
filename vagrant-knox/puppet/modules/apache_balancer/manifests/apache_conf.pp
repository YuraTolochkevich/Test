class apache_balancer::apache_conf ()
{
        config_apache{ 'httpd':  conf_path => "/usr/local/apache2/conf/httpd.conf"}
       
}

define config_apache($conf_path)
{
        file
        { $conf_path:
                path    => $conf_path,
                owner   => root,
                group   => root,
                mode    => 777,
                content => template("apache_balancer/httpd.conf.erb"),

        }

        
}