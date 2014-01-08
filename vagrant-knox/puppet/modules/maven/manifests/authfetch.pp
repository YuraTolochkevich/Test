define maven::authfetch (
  $destination,
  $user,
  $source             = $title,
  $password           = '',
  $timeout            = '0',
  $verbose            = false,
  $redownload         = false,
  $nocheckcertificate = false,
  $execuser           = undef,
) {

  notice("wget::authfetch is deprecated, use wget::fetch with user/password params")

  maven::fetch { $title:
    destination        => $destination,
    source             => $source,
    timeout            => $timeout,
    verbose            => $verbose,
    redownload         => $redownload,
    nocheckcertificate => $nocheckcertificate,
    execuser           => $execuser,
    user               => $user,
    password           => $password,
  }

}