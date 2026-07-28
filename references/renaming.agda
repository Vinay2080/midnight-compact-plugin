-- This file is part of Compact.
-- Copyright (C) 2025 Midnight Foundation
-- SPDX-License-Identifier: Apache-2.0
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
-- 	http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.



module Semantics.Static.Lsrc.Renaming where

open import Semantics.Static.Lsrc.Lsrc-Typing
open import Semantics.Static.Lsrc.Lsrc
open import Syntax.Generated.Lsrc

open import Data.List.Membership.Propositional
open import Data.Product
open import Relation.Unary hiding (_∈_)
open import Data.Sum
open import Data.Maybe

open import Data.List.Relation.Unary.All renaming (map to amap ; lookup to resolve)
open import Data.List hiding (or)
open import Data.List.Relation.Binary.Pointwise hiding (refl)

open import Function

open import Relation.Binary.PropositionalEquality

open import Class.MonotonePredicate

record Rename (T : TypeContext → Set) (R : (𝓒 : IsContext Δ) → T Δ → Set) : Set where
  field
    rename : ∀ {v∗} → Renaming (𝓒 .vars) v∗ → ∀[ R 𝓒 ⇒ R < 𝓒 v↦ v∗ > ]

open Rename ⦃...⦄


mutual
  instance

    rename-expr∗◇
      : Rename (List ∘ ⊢Type) λ 𝓒 → All (◇ (⊢Expr (Δ , 𝓒)))
    rename-expr∗◇ .Rename.rename
      ρ [] = []
    rename-expr∗◇  .Rename.rename
      ρ (◇⟨ ι , ε ⟩ ∷ xs)
      = ◇⟨ ι , (rename ρ ε) ⟩ ∷ (rename ρ xs)

    rename-expr∗vec
      : Rename (List ∘ ⊢Type) λ 𝓒 τ∗ → All (◇ (⊢Expr (Δ , 𝓒) ∘ ⊢vector ζ)) τ∗
    rename-expr∗vec .Rename.rename
      ρ [] = []
    rename-expr∗vec .Rename.rename
      ρ (◇⟨ ι , ε ⟩ ∷ xs)
      = ◇⟨ ι , (rename ρ ε) ⟩ ∷ rename ρ xs

    rename-expr : Rename ⊢Type (λ 𝓒 → ⊢Expr (Δ , 𝓒))

    rename-expr .Rename.rename
      ρ (mkEx _ ⊢expr-quote′)
      = mkEx _ ⊢expr-quote′

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-var-ref x))
      = mkEx _ (⊢expr-var-ref (ρ x))

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-default τ))
      = ⊢default τ

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-if wf wf₁ wf₂ x))
      = ⊢if (rename ρ (mkEx _ wf))
          (rename ρ (mkEx _ wf₁))
          (rename ρ (mkEx _ wf₂))
          (record { ⊢max = x })

    rename-expr .Rename.rename
      ρ (mkEx e ⊢elt-ref)
      = mkEx e ⊢elt-ref

    rename-expr .Rename.rename
      ρ (mkEx e ⊢expr-elt-call)
      = mkEx e ⊢expr-elt-call

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-tuple ε∗))
      = mkEx _ (⊢expr-tuple (rename ρ ε∗))

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-vector ε∗))
      = mkEx _ (⊢expr-vector (rename ρ ε∗))

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-tuple-ref ε px))
      = mkEx _ (⊢expr-tuple-ref (rename ρ ε) px)

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-vector-ref ε i))
      = mkEx _ (⊢expr-vector-ref (rename ρ ε) i)

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-+ wf₁ wf₂))
      = mkEx _ (⊢expr-+
          (rename ρ (mkEx _ wf₁) .well-formed-expression)
          (rename ρ (mkEx _ wf₂) .well-formed-expression))

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-- wf₁ wf₂))
      = mkEx _ (⊢expr--
          (rename ρ (mkEx _ wf₁) .well-formed-expression)
          (rename ρ (mkEx _ wf₂) .well-formed-expression))

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-* wf₁ wf₂))
      = mkEx _ (⊢expr-*
          (rename ρ (mkEx _ wf₁) .well-formed-expression)
          (rename ρ (mkEx _ wf₂) .well-formed-expression))

    rename-expr .Rename.rename
      ρ (mkEx (or e₁ e₂) (⊢expr-or wf₁ wf₂ x))
      = ⊢or (rename ρ (mkEx _ wf₁)) (rename ρ (mkEx _ wf₂)) x

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-and wf₁ wf₂ x))
      = ⊢and (rename ρ (mkEx _ wf₁)) (rename ρ (mkEx _ wf₂)) x

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-not wf))
      = ⊢not (rename ρ (mkEx _ wf))

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-< x y wf₁ wf₂ z))
      = ⊢< (rename ρ (mkEx _ wf₁)) (rename ρ (mkEx _ wf₂)) x y z

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-<= x y wf₁ wf₂ z))
      = ⊢<= (rename ρ (mkEx _ wf₁)) (rename ρ (mkEx _ wf₂)) x y z

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-> x y wf₁ wf₂ z))
      = ⊢> (rename ρ (mkEx _ wf₁)) (rename ρ (mkEx _ wf₂)) x y z

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr->= x y wf₁ wf₂ z))
      = ⊢>= (rename ρ (mkEx _ wf₁)) (rename ρ (mkEx _ wf₂)) x y z

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-== wf₁ wf₂ x))
      = ⊢== (rename ρ (mkEx _ wf₁)) (rename ρ (mkEx _ wf₂)) x

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-!= wf₁ wf₂ x))
      = ⊢!= (rename ρ (mkEx _ wf₁)) (rename ρ (mkEx _ wf₂)) x

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-map {ζ = ζ} fn ◇⟨ ι , ε ⟩ ε∗ eq))
      = subst (⊢Expr _) (cong (⊢vector ζ) return-lemma)
          (⊢map (rename ρ fn) ◇⟨ ι , rename ρ ε ⟩ (rename ρ ε∗) (trans args-lemma eq))

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-fold fn (◇⟨_,_⟩ {x} ι₁ ε-init) ◇⟨ ι₂ , ε ⟩ ε∗ eq))
      = subst (⊢Expr _) return-lemma
          (⊢fold
            (rename ρ fn)
            ◇⟨ subst (x ⊑-⊢_) (sym return-lemma) ι₁ , rename ρ ε-init ⟩
            ◇⟨ ι₂ , (rename ρ ε) ⟩
            (rename ρ ε∗)
            (trans args-lemma (trans eq (cong (_∷ _) (sym return-lemma)))))

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-call fn ε∗))
      = subst (⊢Expr _) return-lemma
          (⊢call (rename ρ fn)
          (subst (All (◇ (⊢Expr _))) (sym args-lemma) (rename ρ ε∗)) )

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-new τ?∗ px m wf args))
      = ⊢new τ?∗ px m wf (rename-new-field∗ ρ args)

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-seq ε∗ wf))
      = ⊢seq (rename ρ ε∗) (rename ρ (mkEx _ wf))

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-cast wf cx))
      = ⊢cast cx (rename ρ (mkEx _ wf))

    rename-expr .Rename.rename
      ρ (mkEx _ (⊢expr-disclose wf))
      = ⊢disclose (rename ρ (mkEx _ wf))

    rename-fun : Rename U λ 𝓒 _ → ⊢Fun (Δ , 𝓒)
    rename-fun .Rename.rename
      ρ (mkFun f (⊢fun-mono fu))
      = mkFun f (⊢fun-mono fu)
    rename-fun .Rename.rename
      ρ (mkFun (fref1 name _) (⊢fun-gen fu m eq))
      = mkFun (fref1 name _) (⊢fun-gen fu m eq)
    rename-fun .Rename.rename {𝓒 = 𝓒} {v∗ = v∗₁}
      ρ (mkFun (circuit x y _) (⊢fun-circuit {v∗ = v∗} ψ∗ τ 𝓢))
      = mkFun (circuit _ _ _) (⊢fun-circuit {v∗ = v∗} ψ∗ τ (rename (rename-++ˡ ρ) 𝓢))

    rename-stmt
      : Rename Variables λ 𝓒 v∗ → ⊢Stmt ((Δ , 𝓒) , τ) v∗
    rename-stmt .Rename.rename
      ρ (mkStmt _ (⊢stmt-expr x eq))
      = mkStmt _ (⊢stmt-expr (rename ρ x) eq)
    rename-stmt .Rename.rename
      ρ (mkStmt _ (⊢stmt-=′ {expr₁ = expr₁} {expr₂ = expr₂}))
      = mkStmt _ (⊢stmt-=′ {expr₁ = expr₁} {expr₂ = expr₂})
    rename-stmt .Rename.rename
      ρ (mkStmt _ (⊢stmt-+= {expr₁ = expr₁} {expr₂ = expr₂}))
      = mkStmt _ (⊢stmt-+= {expr₁ = expr₁} {expr₂ = expr₂})
    rename-stmt .Rename.rename
      ρ (mkStmt _ (⊢stmt--= {expr₁ = expr₁} {expr₂ = expr₂}))
      = mkStmt _ (⊢stmt--= {expr₁ = expr₁} {expr₂ = expr₂})
    rename-stmt .Rename.rename
      ρ (mkStmt _ (⊢stmt-return ◇⟨ ι , ε ⟩))
      = mkStmt _ (⊢stmt-return ◇⟨ ι , (rename ρ ε) ⟩)
    rename-stmt .Rename.rename
      ρ (mkStmt _ (⊢stmt-assert msg x))
      = mkStmt _ (⊢stmt-assert msg (rename ρ x))
    rename-stmt .Rename.rename
      ρ (mkStmt _ (⊢stmt-const v∗ ψ∗ ε))
      = mkStmt _ (⊢stmt-const v∗ ψ∗ (rename ρ ε))
    rename-stmt .Rename.rename
      ρ (mkStmt _ (⊢stmt-if x wf wf₁))
      = mkStmt _ (⊢stmt-if (rename ρ x) (rename ρ wf) (rename ρ wf₁))
    rename-stmt .Rename.rename
      ρ (mkStmt _ (⊢stmt-for name ε 𝓢))
      = mkStmt _ (⊢stmt-for name (rename ρ ε) (rename (rename-++ˡ ρ) 𝓢))
    rename-stmt .Rename.rename
      ρ (mkStmt _ (⊢stmt-block b))
      = mkStmt _ (⊢stmt-block (rename ρ b))

    rename-block
      : Rename Variables λ 𝓒 v∗ → Block ((Δ , 𝓒) , τ) v∗
    rename-block .Rename.rename
      ρ empty = empty
    rename-block .Rename.rename
      ρ (cons x b)
      = cons (rename ρ x) (rename (rename-++ˡ ρ) b)

  rename-new-field
    : ∀ {struct τ?}
    → Renaming (𝓒 .vars) v∗
    → ⊢New-Field ((Δ , 𝓒) , struct) τ?
    → ⊢New-Field ((Δ , < 𝓒 v↦ v∗ >) , struct) τ?
  rename-new-field
    ρ (mkNF _ (⊢new-field-spread m wf ε))
    = mkNF _ (⊢new-field-spread m wf (rename ρ ε))
  rename-new-field
    ρ (mkNF _ (⊢new-field-positional ε))
    = mkNF _ (⊢new-field-positional (rename ρ ε))
  rename-new-field
    ρ (mkNF _ (⊢new-field-named {name = name} ε))
    = mkNF _ (⊢new-field-named {name = name} (rename ρ ε))

  rename-new-field∗
    : ∀ {struct τ?∗}
    → Renaming (𝓒 .vars) v∗
    → All (⊢New-Field ((Δ , 𝓒) , struct)) τ?∗
    → All (⊢New-Field ((Δ , < 𝓒 v↦ v∗ >) , struct)) τ?∗
  rename-new-field∗ ρ []
    = []
  rename-new-field∗ ρ (nf ∷ xs)
    = rename-new-field ρ nf ∷ rename-new-field∗ ρ xs

  return-lemma
    : ∀ {fn : ⊢Fun Γ} {v∗} {ρ : Renaming (𝓒⟨ Γ ⟩ .vars) v∗}
    → returnT (rename ρ fn) ≡ returnT fn
  return-lemma {fn = mkFun f (⊢fun-mono x)}
    = refl
  return-lemma {Γ = Γ} {fn = mkFun (fref1 name x) (⊢fun-gen {κ = κ} fu m ts)} {ρ = ρ}
    = refl
  return-lemma {fn = mkFun f (⊢fun-circuit ψ∗ τ x)}
    = refl

  args-lemma
    : ∀ {fn : ⊢Fun Γ} {v∗}
      {ρ : Renaming (𝓒⟨ Γ ⟩ .vars) v∗}
    → argsT (rename ρ fn) ≡ argsT fn
  args-lemma {Γ} {mkFun f (⊢fun-mono x)} {v∗} {ρ}
    = refl
  args-lemma {Γ} {mkFun f (⊢fun-gen x x₁ x₂)} {v∗} {ρ}
    = refl
  args-lemma {Γ} {mkFun f (⊢fun-circuit ψ∗ τ x)} {v∗} {ρ}
    = refl
