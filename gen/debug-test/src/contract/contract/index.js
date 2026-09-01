import * as __compactRuntime from '@midnight-ntwrk/compact-runtime';
__compactRuntime.checkRuntimeVersion('0.16.0');

const _descriptor_0 = __compactRuntime.CompactTypeBoolean;

const _descriptor_1 = new __compactRuntime.CompactTypeBytes(32);

class _Maybe_0 {
  alignment() {
    return _descriptor_0.alignment().concat(_descriptor_1.alignment());
  }
  fromValue(value_0) {
    return {
      is_some: _descriptor_0.fromValue(value_0),
      value: _descriptor_1.fromValue(value_0)
    }
  }
  toValue(value_0) {
    return _descriptor_0.toValue(value_0.is_some).concat(_descriptor_1.toValue(value_0.value));
  }
}

const _descriptor_2 = new _Maybe_0();

const _descriptor_3 = new __compactRuntime.CompactTypeUnsignedInteger(4095n, 2);

class _Maybe_1 {
  alignment() {
    return _descriptor_0.alignment().concat(_descriptor_3.alignment());
  }
  fromValue(value_0) {
    return {
      is_some: _descriptor_0.fromValue(value_0),
      value: _descriptor_3.fromValue(value_0)
    }
  }
  toValue(value_0) {
    return _descriptor_0.toValue(value_0.is_some).concat(_descriptor_3.toValue(value_0.value));
  }
}

const _descriptor_4 = new _Maybe_1();

const _descriptor_5 = new __compactRuntime.CompactTypeUnsignedInteger(65535n, 2);

const _descriptor_6 = new __compactRuntime.CompactTypeUnsignedInteger(18446744073709551615n, 8);

class _Either_0 {
  alignment() {
    return _descriptor_0.alignment().concat(_descriptor_1.alignment().concat(_descriptor_1.alignment()));
  }
  fromValue(value_0) {
    return {
      is_left: _descriptor_0.fromValue(value_0),
      left: _descriptor_1.fromValue(value_0),
      right: _descriptor_1.fromValue(value_0)
    }
  }
  toValue(value_0) {
    return _descriptor_0.toValue(value_0.is_left).concat(_descriptor_1.toValue(value_0.left).concat(_descriptor_1.toValue(value_0.right)));
  }
}

const _descriptor_7 = new _Either_0();

const _descriptor_8 = new __compactRuntime.CompactTypeUnsignedInteger(340282366920938463463374607431768211455n, 16);

class _ContractAddress_0 {
  alignment() {
    return _descriptor_1.alignment();
  }
  fromValue(value_0) {
    return {
      bytes: _descriptor_1.fromValue(value_0)
    }
  }
  toValue(value_0) {
    return _descriptor_1.toValue(value_0.bytes);
  }
}

const _descriptor_9 = new _ContractAddress_0();

const _descriptor_10 = new __compactRuntime.CompactTypeUnsignedInteger(255n, 1);

export class Contract {
  witnesses;
  constructor(...args_0) {
    if (args_0.length !== 1) {
      throw new __compactRuntime.CompactError(`Contract constructor: expected 1 argument, received ${args_0.length}`);
    }
    const witnesses_0 = args_0[0];
    if (typeof(witnesses_0) !== 'object') {
      throw new __compactRuntime.CompactError('first (witnesses) argument to Contract constructor is not an object');
    }
    if (typeof(witnesses_0.compute) !== 'function') {
      throw new __compactRuntime.CompactError('first (witnesses) argument to Contract constructor does not contain a function-valued field named compute');
    }
    this.witnesses = witnesses_0;
    this.circuits = {
      ledgerCalls: (...args_1) => {
        if (args_1.length !== 2) {
          throw new __compactRuntime.CompactError(`ledgerCalls: expected 2 arguments (as invoked from Typescript), received ${args_1.length}`);
        }
        const contextOrig_0 = args_1[0];
        const guess_0 = args_1[1];
        if (!(typeof(contextOrig_0) === 'object' && contextOrig_0.currentQueryContext != undefined)) {
          __compactRuntime.typeError('ledgerCalls',
                                     'argument 1 (as invoked from Typescript)',
                                     'contract.compact line 39 char 1',
                                     'CircuitContext',
                                     contextOrig_0)
        }
        if (!(typeof(guess_0) === 'bigint' && guess_0 >= 0n && guess_0 <= 4095n)) {
          __compactRuntime.typeError('ledgerCalls',
                                     'argument 1 (argument 2 as invoked from Typescript)',
                                     'contract.compact line 39 char 1',
                                     'Uint<0..4096>',
                                     guess_0)
        }
        const context = { ...contextOrig_0, gasCost: __compactRuntime.emptyRunningCost() };
        const partialProofData = {
          input: {
            value: _descriptor_3.toValue(guess_0),
            alignment: _descriptor_3.alignment()
          },
          output: undefined,
          publicTranscript: [],
          privateTranscriptOutputs: []
        };
        const result_0 = this._ledgerCalls_0(context, partialProofData, guess_0);
        partialProofData.output = { value: [], alignment: [] };
        return { result: result_0, context: context, proofData: partialProofData, gasCost: context.gasCost };
      },
      nestedCall: (...args_1) => {
        if (args_1.length !== 3) {
          throw new __compactRuntime.CompactError(`nestedCall: expected 3 arguments (as invoked from Typescript), received ${args_1.length}`);
        }
        const contextOrig_0 = args_1[0];
        const fst_0 = args_1[1];
        const snd_0 = args_1[2];
        if (!(typeof(contextOrig_0) === 'object' && contextOrig_0.currentQueryContext != undefined)) {
          __compactRuntime.typeError('nestedCall',
                                     'argument 1 (as invoked from Typescript)',
                                     'contract.compact line 57 char 1',
                                     'CircuitContext',
                                     contextOrig_0)
        }
        if (!(typeof(fst_0) === 'boolean')) {
          __compactRuntime.typeError('nestedCall',
                                     'argument 1 (argument 2 as invoked from Typescript)',
                                     'contract.compact line 57 char 1',
                                     'Boolean',
                                     fst_0)
        }
        if (!(typeof(snd_0) === 'boolean')) {
          __compactRuntime.typeError('nestedCall',
                                     'argument 2 (argument 3 as invoked from Typescript)',
                                     'contract.compact line 57 char 1',
                                     'Boolean',
                                     snd_0)
        }
        const context = { ...contextOrig_0, gasCost: __compactRuntime.emptyRunningCost() };
        const partialProofData = {
          input: {
            value: _descriptor_0.toValue(fst_0).concat(_descriptor_0.toValue(snd_0)),
            alignment: _descriptor_0.alignment().concat(_descriptor_0.alignment())
          },
          output: undefined,
          publicTranscript: [],
          privateTranscriptOutputs: []
        };
        const result_0 = this._nestedCall_0(context,
                                            partialProofData,
                                            fst_0,
                                            snd_0);
        partialProofData.output = { value: _descriptor_4.toValue(result_0), alignment: _descriptor_4.alignment() };
        return { result: result_0, context: context, proofData: partialProofData, gasCost: context.gasCost };
      },
      privateCall: (...args_1) => {
        if (args_1.length !== 3) {
          throw new __compactRuntime.CompactError(`privateCall: expected 3 arguments (as invoked from Typescript), received ${args_1.length}`);
        }
        const contextOrig_0 = args_1[0];
        const fst_0 = args_1[1];
        const snd_0 = args_1[2];
        if (!(typeof(contextOrig_0) === 'object' && contextOrig_0.currentQueryContext != undefined)) {
          __compactRuntime.typeError('privateCall',
                                     'argument 1 (as invoked from Typescript)',
                                     'contract.compact line 65 char 1',
                                     'CircuitContext',
                                     contextOrig_0)
        }
        if (!(typeof(fst_0) === 'boolean')) {
          __compactRuntime.typeError('privateCall',
                                     'argument 1 (argument 2 as invoked from Typescript)',
                                     'contract.compact line 65 char 1',
                                     'Boolean',
                                     fst_0)
        }
        if (!(typeof(snd_0) === 'boolean')) {
          __compactRuntime.typeError('privateCall',
                                     'argument 2 (argument 3 as invoked from Typescript)',
                                     'contract.compact line 65 char 1',
                                     'Boolean',
                                     snd_0)
        }
        const context = { ...contextOrig_0, gasCost: __compactRuntime.emptyRunningCost() };
        const partialProofData = {
          input: {
            value: _descriptor_0.toValue(fst_0).concat(_descriptor_0.toValue(snd_0)),
            alignment: _descriptor_0.alignment().concat(_descriptor_0.alignment())
          },
          output: undefined,
          publicTranscript: [],
          privateTranscriptOutputs: []
        };
        const result_0 = this._privateCall_0(context,
                                             partialProofData,
                                             fst_0,
                                             snd_0);
        partialProofData.output = { value: _descriptor_4.toValue(result_0), alignment: _descriptor_4.alignment() };
        return { result: result_0, context: context, proofData: partialProofData, gasCost: context.gasCost };
      },
      stdLibCall(context, ...args_1) {
        return { result: pureCircuits.stdLibCall(...args_1), context };
      }
    };
    this.impureCircuits = {
      ledgerCalls: this.circuits.ledgerCalls,
      nestedCall: this.circuits.nestedCall,
      privateCall: this.circuits.privateCall
    };
    this.provableCircuits = {
      ledgerCalls: this.circuits.ledgerCalls,
      nestedCall: this.circuits.nestedCall,
      privateCall: this.circuits.privateCall
    };
  }
  initialState(...args_0) {
    if (args_0.length !== 2) {
      throw new __compactRuntime.CompactError(`Contract state constructor: expected 2 arguments (as invoked from Typescript), received ${args_0.length}`);
    }
    const constructorContext_0 = args_0[0];
    const difficulty_0 = args_0[1];
    if (typeof(constructorContext_0) !== 'object') {
      throw new __compactRuntime.CompactError(`Contract state constructor: expected 'constructorContext' in argument 1 (as invoked from Typescript) to be an object`);
    }
    if (!('initialPrivateState' in constructorContext_0)) {
      throw new __compactRuntime.CompactError(`Contract state constructor: expected 'initialPrivateState' in argument 1 (as invoked from Typescript)`);
    }
    if (!('initialZswapLocalState' in constructorContext_0)) {
      throw new __compactRuntime.CompactError(`Contract state constructor: expected 'initialZswapLocalState' in argument 1 (as invoked from Typescript)`);
    }
    if (typeof(constructorContext_0.initialZswapLocalState) !== 'object') {
      throw new __compactRuntime.CompactError(`Contract state constructor: expected 'initialZswapLocalState' in argument 1 (as invoked from Typescript) to be an object`);
    }
    if (!(typeof(difficulty_0) === 'bigint' && difficulty_0 >= 0n && difficulty_0 <= 4095n)) {
      __compactRuntime.typeError('Contract state constructor',
                                 'argument 1 (argument 2 as invoked from Typescript)',
                                 'contract.compact line 24 char 1',
                                 'Uint<0..4096>',
                                 difficulty_0)
    }
    const state_0 = new __compactRuntime.ContractState();
    let stateValue_0 = __compactRuntime.StateValue.newArray();
    stateValue_0 = stateValue_0.arrayPush(__compactRuntime.StateValue.newNull());
    stateValue_0 = stateValue_0.arrayPush(__compactRuntime.StateValue.newNull());
    state_0.data = new __compactRuntime.ChargedState(stateValue_0);
    state_0.setOperation('ledgerCalls', new __compactRuntime.ContractOperation());
    state_0.setOperation('nestedCall', new __compactRuntime.ContractOperation());
    state_0.setOperation('privateCall', new __compactRuntime.ContractOperation());
    const context = __compactRuntime.createCircuitContext(__compactRuntime.dummyContractAddress(), constructorContext_0.initialZswapLocalState.coinPublicKey, state_0.data, constructorContext_0.initialPrivateState);
    const partialProofData = {
      input: { value: [], alignment: [] },
      output: undefined,
      publicTranscript: [],
      privateTranscriptOutputs: []
    };
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_10.toValue(0n),
                                                                                              alignment: _descriptor_10.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_6.toValue(0n),
                                                                                              alignment: _descriptor_6.alignment() }).encode() } },
                                       { ins: { cached: false, n: 1 } }]);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_10.toValue(1n),
                                                                                              alignment: _descriptor_10.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_3.toValue(0n),
                                                                                              alignment: _descriptor_3.alignment() }).encode() } },
                                       { ins: { cached: false, n: 1 } }]);
    if (this._equal_0(difficulty_0, 0n)) {
      const result_0 = 42n;
      const tmp_0 = result_0;
      __compactRuntime.queryLedgerState(context,
                                        partialProofData,
                                        [
                                         { push: { storage: false,
                                                   value: __compactRuntime.StateValue.newCell({ value: _descriptor_10.toValue(1n),
                                                                                                alignment: _descriptor_10.alignment() }).encode() } },
                                         { push: { storage: true,
                                                   value: __compactRuntime.StateValue.newCell({ value: _descriptor_3.toValue(tmp_0),
                                                                                                alignment: _descriptor_3.alignment() }).encode() } },
                                         { ins: { cached: false, n: 1 } }]);
    } else {
      __compactRuntime.queryLedgerState(context,
                                        partialProofData,
                                        [
                                         { push: { storage: false,
                                                   value: __compactRuntime.StateValue.newCell({ value: _descriptor_10.toValue(1n),
                                                                                                alignment: _descriptor_10.alignment() }).encode() } },
                                         { push: { storage: true,
                                                   value: __compactRuntime.StateValue.newCell({ value: _descriptor_3.toValue(difficulty_0),
                                                                                                alignment: _descriptor_3.alignment() }).encode() } },
                                         { ins: { cached: false, n: 1 } }]);
    }
    state_0.data = new __compactRuntime.ChargedState(context.currentQueryContext.state.state);
    return {
      currentContractState: state_0,
      currentPrivateState: context.currentPrivateState,
      currentZswapLocalState: context.currentZswapLocalState
    }
  }
  _some_0(value_0) { return { is_some: true, value: value_0 }; }
  _some_1(value_0) { return { is_some: true, value: value_0 }; }
  _none_0() { return { is_some: false, value: 0n }; }
  _persistentCommit_0(value_0, rand_0) {
    const result_0 = __compactRuntime.persistentCommit(_descriptor_3,
                                                       value_0,
                                                       rand_0);
    return result_0;
  }
  _compute_0(context, partialProofData, fst_0, snd_0) {
    const witnessContext_0 = __compactRuntime.createWitnessContext(ledger(context.currentQueryContext.state), context.currentPrivateState, context.currentQueryContext.address);
    const [nextPrivateState_0, result_0] = this.witnesses.compute(witnessContext_0,
                                                                  fst_0,
                                                                  snd_0);
    context.currentPrivateState = nextPrivateState_0;
    if (!(typeof(result_0) === 'boolean')) {
      __compactRuntime.typeError('compute',
                                 'return value',
                                 'contract.compact line 35 char 1',
                                 'Boolean',
                                 result_0)
    }
    partialProofData.privateTranscriptOutputs.push({
      value: _descriptor_0.toValue(result_0),
      alignment: _descriptor_0.alignment()
    });
    return result_0;
  }
  _ledgerCalls_0(context, partialProofData, guess_0) {
    __compactRuntime.assert(!this._equal_1(_descriptor_3.fromValue(__compactRuntime.queryLedgerState(context,
                                                                                                     partialProofData,
                                                                                                     [
                                                                                                      { dup: { n: 0 } },
                                                                                                      { idx: { cached: false,
                                                                                                               pushPath: false,
                                                                                                               path: [
                                                                                                                      { tag: 'value',
                                                                                                                        value: { value: _descriptor_10.toValue(1n),
                                                                                                                                 alignment: _descriptor_10.alignment() } }] } },
                                                                                                      { popeq: { cached: false,
                                                                                                                 result: undefined } }]).value),
                                           _descriptor_6.fromValue(__compactRuntime.queryLedgerState(context,
                                                                                                     partialProofData,
                                                                                                     [
                                                                                                      { dup: { n: 0 } },
                                                                                                      { idx: { cached: false,
                                                                                                               pushPath: false,
                                                                                                               path: [
                                                                                                                      { tag: 'value',
                                                                                                                        value: { value: _descriptor_10.toValue(0n),
                                                                                                                                 alignment: _descriptor_10.alignment() } }] } },
                                                                                                      { popeq: { cached: true,
                                                                                                                 result: undefined } }]).value)),
                            'all your base are belong to us');
    __compactRuntime.assert(!this._equal_2(guess_0, 0n),
                            'guess should be above 0');
    const tmp_0 = 1n;
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { idx: { cached: false,
                                                pushPath: true,
                                                path: [
                                                       { tag: 'value',
                                                         value: { value: _descriptor_10.toValue(0n),
                                                                  alignment: _descriptor_10.alignment() } }] } },
                                       { addi: { immediate: parseInt(__compactRuntime.valueToBigInt(
                                                              { value: _descriptor_5.toValue(tmp_0),
                                                                alignment: _descriptor_5.alignment() }
                                                                .value
                                                            )) } },
                                       { ins: { cached: true, n: 1 } }]);
    return [];
  }
  _either_0(fst_0, snd_0) {
    if (fst_0 === true) {
      const result_0 = snd_0 === false; return result_0;
    } else {
      return snd_0;
    }
  }
  _nestedCall_0(context, partialProofData, fst_0, snd_0) {
    const resCond_0 = this._either_0(fst_0, snd_0);
    if (resCond_0) {
      return this._some_0(_descriptor_3.fromValue(__compactRuntime.queryLedgerState(context,
                                                                                    partialProofData,
                                                                                    [
                                                                                     { dup: { n: 0 } },
                                                                                     { idx: { cached: false,
                                                                                              pushPath: false,
                                                                                              path: [
                                                                                                     { tag: 'value',
                                                                                                       value: { value: _descriptor_10.toValue(1n),
                                                                                                                alignment: _descriptor_10.alignment() } }] } },
                                                                                     { popeq: { cached: false,
                                                                                                result: undefined } }]).value));
    } else {
      return this._none_0();
    }
  }
  _privateCall_0(context, partialProofData, fst_0, snd_0) {
    const resCond_0 = this._compute_0(context, partialProofData, fst_0, snd_0);
    if (resCond_0) {
      return this._some_0(_descriptor_3.fromValue(__compactRuntime.queryLedgerState(context,
                                                                                    partialProofData,
                                                                                    [
                                                                                     { dup: { n: 0 } },
                                                                                     { idx: { cached: false,
                                                                                              pushPath: false,
                                                                                              path: [
                                                                                                     { tag: 'value',
                                                                                                       value: { value: _descriptor_10.toValue(1n),
                                                                                                                alignment: _descriptor_10.alignment() } }] } },
                                                                                     { popeq: { cached: false,
                                                                                                result: undefined } }]).value));
    } else {
      return this._none_0();
    }
  }
  _stdLibCall_0(fst_0, snd_0) {
    if (fst_0) {
      const rand_0 = new Uint8Array([49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50]);
      const valueToHash_0 = 1n;
      const hash_0 = this._persistentCommit_0(valueToHash_0, rand_0);
      return this._some_1(rand_0);
    } else {
      const rand_1 = new Uint8Array([49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49]);
      const valueToHash_1 = 42n;
      const hash_1 = this._persistentCommit_0(valueToHash_1, rand_1);
      return this._some_1(hash_1);
    }
  }
  _equal_0(x0, y0) {
    if (x0 !== y0) { return false; }
    return true;
  }
  _equal_1(x0, y0) {
    if (x0 !== y0) { return false; }
    return true;
  }
  _equal_2(x0, y0) {
    if (x0 !== y0) { return false; }
    return true;
  }
}
export function ledger(stateOrChargedState) {
  const state = stateOrChargedState instanceof __compactRuntime.StateValue ? stateOrChargedState : stateOrChargedState.state;
  const chargedState = stateOrChargedState instanceof __compactRuntime.StateValue ? new __compactRuntime.ChargedState(stateOrChargedState) : stateOrChargedState;
  const context = {
    currentQueryContext: new __compactRuntime.QueryContext(chargedState, __compactRuntime.dummyContractAddress()),
    costModel: __compactRuntime.CostModel.initialCostModel()
  };
  const partialProofData = {
    input: { value: [], alignment: [] },
    output: undefined,
    publicTranscript: [],
    privateTranscriptOutputs: []
  };
  return {
    get attempts() {
      return _descriptor_6.fromValue(__compactRuntime.queryLedgerState(context,
                                                                       partialProofData,
                                                                       [
                                                                        { dup: { n: 0 } },
                                                                        { idx: { cached: false,
                                                                                 pushPath: false,
                                                                                 path: [
                                                                                        { tag: 'value',
                                                                                          value: { value: _descriptor_10.toValue(0n),
                                                                                                   alignment: _descriptor_10.alignment() } }] } },
                                                                        { popeq: { cached: true,
                                                                                   result: undefined } }]).value);
    },
    get maxAttempts() {
      return _descriptor_3.fromValue(__compactRuntime.queryLedgerState(context,
                                                                       partialProofData,
                                                                       [
                                                                        { dup: { n: 0 } },
                                                                        { idx: { cached: false,
                                                                                 pushPath: false,
                                                                                 path: [
                                                                                        { tag: 'value',
                                                                                          value: { value: _descriptor_10.toValue(1n),
                                                                                                   alignment: _descriptor_10.alignment() } }] } },
                                                                        { popeq: { cached: false,
                                                                                   result: undefined } }]).value);
    }
  };
}
const _emptyContext = {
  currentQueryContext: new __compactRuntime.QueryContext(new __compactRuntime.ContractState().data, __compactRuntime.dummyContractAddress())
};
const _dummyContract = new Contract({ compute: (...args) => undefined });
export const pureCircuits = {
  stdLibCall: (...args_0) => {
    if (args_0.length !== 2) {
      throw new __compactRuntime.CompactError(`stdLibCall: expected 2 arguments (as invoked from Typescript), received ${args_0.length}`);
    }
    const fst_0 = args_0[0];
    const snd_0 = args_0[1];
    if (!(typeof(fst_0) === 'boolean')) {
      __compactRuntime.typeError('stdLibCall',
                                 'argument 1',
                                 'contract.compact line 73 char 1',
                                 'Boolean',
                                 fst_0)
    }
    if (!(typeof(snd_0) === 'boolean')) {
      __compactRuntime.typeError('stdLibCall',
                                 'argument 2',
                                 'contract.compact line 73 char 1',
                                 'Boolean',
                                 snd_0)
    }
    return _dummyContract._stdLibCall_0(fst_0, snd_0);
  }
};
export const contractReferenceLocations =
  { tag: 'publicLedgerArray', indices: { } };
//# sourceMappingURL=index.js.map
