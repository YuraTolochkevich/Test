class knox_install::knox_build() {
  

class {'knox_download':
  cwd => "/",
  url => "https://dist.apache.org/repos/dist/dev/incubator/knox/knox-incubating-0.3.1/",
  file_name => "knox-incubating-0.3.1",
   }

class {'gateway_conf':

 }
 class {'sandbox_conf':}
 
class {'gateway_sh_conf':
}
 class{'knox_start':} 

}
  
  
 

