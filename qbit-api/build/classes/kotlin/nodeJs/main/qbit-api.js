(function (root, factory) {
  if (typeof define === 'function' && define.amd)
    define(['exports', 'kotlin'], factory);
  else if (typeof exports === 'object')
    factory(module.exports, require('kotlin'));
  else {
    if (typeof kotlin === 'undefined') {
      throw new Error("Error loading module 'qbit-api'. Its dependency 'kotlin' was not found. Please, check whether 'kotlin' is loaded prior to 'qbit-api'.");
    }
    root['qbit-api'] = factory(typeof this['qbit-api'] === 'undefined' ? {} : this['qbit-api'], kotlin);
  }
}(this, function (_, Kotlin) {
  'use strict';
  var Kind_CLASS = Kotlin.Kind.CLASS;
  function Conn() {
  }
  Conn.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Conn',
    interfaces: []
  };
  var package$qbit = _.qbit || (_.qbit = {});
  package$qbit.Conn = Conn;
  Kotlin.defineModule('qbit-api', _);
  return _;
}));

//# sourceMappingURL=qbit-api.js.map
