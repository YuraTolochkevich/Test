
class wget (
  $version = present,
) {

  if $::operatingsystem != 'Darwin' {
    if ! defined(Package['wget']) {
      package { 'wget': ensure => $version }
    }
  }
}
