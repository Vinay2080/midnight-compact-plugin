import type * as __compactRuntime from '@midnight-ntwrk/compact-runtime';

export type Maybe<T> = { is_some: boolean; value: T };

export type Witnesses<PS> = {
  compute(context: __compactRuntime.WitnessContext<Ledger, PS>,
          fst_0: boolean,
          snd_0: boolean): [PS, boolean];
}

export type ImpureCircuits<PS> = {
  ledgerCalls(context: __compactRuntime.CircuitContext<PS>, guess_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  nestedCall(context: __compactRuntime.CircuitContext<PS>,
             fst_0: boolean,
             snd_0: boolean): __compactRuntime.CircuitResults<PS, Maybe<bigint>>;
  privateCall(context: __compactRuntime.CircuitContext<PS>,
              fst_0: boolean,
              snd_0: boolean): __compactRuntime.CircuitResults<PS, Maybe<bigint>>;
}

export type ProvableCircuits<PS> = {
  ledgerCalls(context: __compactRuntime.CircuitContext<PS>, guess_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  nestedCall(context: __compactRuntime.CircuitContext<PS>,
             fst_0: boolean,
             snd_0: boolean): __compactRuntime.CircuitResults<PS, Maybe<bigint>>;
  privateCall(context: __compactRuntime.CircuitContext<PS>,
              fst_0: boolean,
              snd_0: boolean): __compactRuntime.CircuitResults<PS, Maybe<bigint>>;
}

export type PureCircuits = {
  stdLibCall(fst_0: boolean, snd_0: boolean): Maybe<Uint8Array>;
}

export type Circuits<PS> = {
  ledgerCalls(context: __compactRuntime.CircuitContext<PS>, guess_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  nestedCall(context: __compactRuntime.CircuitContext<PS>,
             fst_0: boolean,
             snd_0: boolean): __compactRuntime.CircuitResults<PS, Maybe<bigint>>;
  privateCall(context: __compactRuntime.CircuitContext<PS>,
              fst_0: boolean,
              snd_0: boolean): __compactRuntime.CircuitResults<PS, Maybe<bigint>>;
  stdLibCall(context: __compactRuntime.CircuitContext<PS>,
             fst_0: boolean,
             snd_0: boolean): __compactRuntime.CircuitResults<PS, Maybe<Uint8Array>>;
}

export type Ledger = {
  readonly attempts: bigint;
  readonly maxAttempts: bigint;
}

export type ContractReferenceLocations = any;

export declare const contractReferenceLocations : ContractReferenceLocations;

export declare class Contract<PS = any, W extends Witnesses<PS> = Witnesses<PS>> {
  witnesses: W;
  circuits: Circuits<PS>;
  impureCircuits: ImpureCircuits<PS>;
  provableCircuits: ProvableCircuits<PS>;
  constructor(witnesses: W);
  initialState(context: __compactRuntime.ConstructorContext<PS>,
               difficulty_0: bigint): __compactRuntime.ConstructorResult<PS>;
}

export declare function ledger(state: __compactRuntime.StateValue | __compactRuntime.ChargedState): Ledger;
export declare const pureCircuits: PureCircuits;
