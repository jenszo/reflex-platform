# Use newer aeson because it is not compatible to the old one!
# This package can be removed once aeson in reflex-platform is higher than 1.0.
{ stdenv, mkDerivation, attoparsec, base, base-compat, base-orphans
, base16-bytestring, bytestring, containers, deepseq, directory
, dlist, filepath, generic-deriving, ghc-prim, hashable, integer-logarithms
, hashable-time, HUnit, QuickCheck, quickcheck-instances
, scientific, tagged, template-haskell, test-framework
, test-framework-hunit, test-framework-quickcheck2, text, time
, time-locale-compat, unordered-containers, uuid-types, vector, aeson-use-cffi ? true
}:
     mkDerivation {
       pname = "aeson";
       version = "1.1.1.0";
       sha256 = "1mkj4a09x9psmgq9sg5nz9va76756zfm97ds2gk2qpgxc7nr2dq8";
       configureFlags = if aeson-use-cffi then [] else [ "-f-cffi"];
       libraryHaskellDepends = [
         attoparsec base base-compat bytestring containers deepseq dlist
         ghc-prim hashable scientific tagged template-haskell text time
         time-locale-compat unordered-containers uuid-types vector integer-logarithms
       ];
       testHaskellDepends = [
         attoparsec base base-compat base-orphans base16-bytestring
         bytestring containers directory dlist filepath generic-deriving
         ghc-prim hashable hashable-time HUnit QuickCheck
         quickcheck-instances scientific tagged template-haskell
         test-framework test-framework-hunit test-framework-quickcheck2 text
         time time-locale-compat unordered-containers uuid-types vector integer-logarithms
       ];
       homepage = "https://github.com/bos/aeson";
       description = "Fast JSON parsing and encoding";
       license = stdenv.lib.licenses.bsd3;
       hydraPlatforms = stdenv.lib.platforms.none;
}
