{ stdenv, mkDerivation, attoparsec, base, base-compat, base-orphans
, base16-bytestring, bytestring, containers, deepseq, dlist
, generic-deriving, ghc-prim, hashable, hashable-time, HUnit
, QuickCheck, quickcheck-instances, scientific, tagged
, template-haskell, test-framework, test-framework-hunit
, test-framework-quickcheck2, text, time, time-locale-compat
, unordered-containers, vector
}:
  mkDerivation {
    pname = "aeson";
    version = "1.0.2.1";
    sha256 = "e0a66fba0a9996063d0e241b0b868c6271b6aeb457821a78bfcaac5d84c89066";
    libraryHaskellDepends = [
      attoparsec base base-compat bytestring containers deepseq dlist
      ghc-prim hashable scientific tagged template-haskell text time
      time-locale-compat unordered-containers vector
    ];
    testHaskellDepends = [
      attoparsec base base-compat base-orphans base16-bytestring
      bytestring containers dlist generic-deriving ghc-prim hashable
      hashable-time HUnit QuickCheck quickcheck-instances scientific
      tagged template-haskell test-framework test-framework-hunit
      test-framework-quickcheck2 text time time-locale-compat
      unordered-containers vector
    ];
    homepage = "https://github.com/bos/aeson";
    description = "Fast JSON parsing and encoding";
    license = stdenv.lib.licenses.bsd3;
  }
