-- Donnees initiales pour Fiscal-Track - Annee 2026
-- Ces valeurs sont basees sur CDI 2024, CPF, IR 2026, IS 2026
-- Format : code, valeur, annee_debut, annee_fin, base_legale, secteur, description

-- ============================================
-- PENALITES (CDI 20.01.53)
-- ============================================
INSERT INTO parametres_fiscaux (code, valeur, annee_debut, annee_fin, base_legale, secteur, description) VALUES
('PENALITE_SEUIL_GRANDE_ENTREPRISE', 400000000, 2026, null, 'CDI 20.01.53', 'PENALITE', 'Seuil CA grande entreprise (>= 400M)'),
('PENALITE_SEUIL_MOYENNE_ENTREPRISE', 100000000, 2026, null, 'CDI 20.01.53', 'PENALITE', 'Seuil CA moyenne entreprise (>= 100M)'),
('PENALITE_TAUX_INITIAL_PETITE', 0.01, 2026, null, 'CDI 20.01.53', 'PENALITE', 'Taux initial pour CA < 100M'),
('PENALITE_TAUX_INITIAL_MOYENNE', 0.02, 2026, null, 'CDI 20.01.53', 'PENALITE', 'Taux initial pour CA 100M - 400M'),
('PENALITE_TAUX_INITIAL_GRANDE', 0.03, 2026, null, 'CDI 20.01.53', 'PENALITE', 'Taux initial pour CA >= 400M'),
('PENALITE_TAUX_INCREMENT_MENSUEL', 0.01, 2026, null, 'CDI 20.01.53', 'PENALITE', 'Increment mensuel apres le 1er mois'),
('PENALITE_PLANCHER', 10000, 2026, null, 'CDI 20.01.53', 'PENALITE', 'Plancher minimum de penalite')
ON CONFLICT (code) DO NOTHING;
-- ============================================
-- AMENDE DE DEFAUT DE DEPOT (CDI 20.01.52)
-- ============================================
INSERT INTO parametres_fiscaux (code, valeur, annee_debut, annee_fin, base_legale, secteur, description) VALUES
('DEFAUT_DEPOT_GRANDE_ENTREPRISE', 200000, 2026, null, 'CDI 20.01.52', 'DEFAUT_DEPOT', 'Amende pour CA >= 400M'),
('DEFAUT_DEPOT_MOYENNE_ENTREPRISE', 100000, 2026, null, 'CDI 20.01.52', 'DEFAUT_DEPOT', 'Amende pour CA 200M - 400M'),
('DEFAUT_DEPOT_PETITE_ENTREPRISE', 50000, 2026, null, 'CDI 20.01.52', 'DEFAUT_DEPOT', 'Amende pour CA 50M - 200M et associations'),
('DEFAUT_DEPOT_MICRO_ENTREPRISE', 20000, 2026, null, 'CDI 20.01.52', 'DEFAUT_DEPOT', 'Amende pour CA < 50M')
ON CONFLICT (code) DO NOTHING;
-- ============================================
-- IR - Impot sur les Revenus (CDI 01.01.05, 01.01.14, 01.01.15)
-- ============================================
INSERT INTO parametres_fiscaux (code, valeur, annee_debut, annee_fin, base_legale, secteur, description) VALUES
('IR_TAUX_NORMAL', 0.20, 2026, null, 'CDI 01.01.05', 'IR', 'Taux normal IR (20%)'),
('IR_TAUX_EDUCATION_SANTE', 0.10, 2026, null, 'CDI 01.01.05 V / 01.01.14 V', 'IR', 'Taux reduit pour EDUCATION et SANTE (10%)'),
('IR_TAUX_CARBURANT', 0.002, 2026, null, 'CDI 01.01.05 VI', 'IR', 'Minimum de perception carburant au detail (2 pour mille du CA)'),
('IR_PART_FIXE_AGRICOLE', 500000, 2026, null, 'CDI 01.01.05', 'IR', 'Part fixe pour categories AGRICOLE, ARTISANAL, INDUSTRIEL, MINIER, HOTELIER, TOURISTIQUE'),
('IR_PART_FIXE_AUTRES', 1000000, 2026, null, 'CDI 01.01.05', 'IR', 'Part fixe pour category AUTRES'),
('IR_PART_FIXE_EDUCATION_SANTE', 200000, 2026, null, 'CDI 01.01.05 V / 01.01.14 V', 'IR', 'Part fixe pour EDUCATION et SANTE'),
('IR_TAUX_VARIABLE_AGRICOLE', 0.01, 2026, null, 'CDI 01.01.05', 'IR', 'Taux variable (1%) pour AGRICOLE, ARTISANAL, INDUSTRIEL, MINIER, HOTELIER, TOURISTIQUE'),
('IR_TAUX_VARIABLE_EDUCATION', 0.002, 2026, null, 'CDI 01.01.05 V / 01.01.14 V', 'IR', 'Taux variable (0.2%) pour EDUCATION et SANTE'),
('IR_MINIMUM_SARL', 320000, 2026, null, 'CDI 01.01.05', 'IR', 'Minimum pour ASSOCIE_GERANT_SARL'),
('IR_ABATTEMENT_SARL', 4200000, 2026, null, 'CDI 01.01.05', 'IR', 'Abattement pour ASSOCIE_GERANT_SARL')
ON CONFLICT (code) DO NOTHING;
-- ============================================
-- IS - Impot Synthetique (CDI 01.02.05 bis)
-- ============================================
INSERT INTO parametres_fiscaux (code, valeur, annee_debut, annee_fin, base_legale, secteur, description) VALUES
('IS_TAUX', 0.05, 2026, null, 'CDI 01.02.05', 'IS', 'Taux normal IS (5%)'),
('IS_TAUX_ABATTEMENT_CGA', 0.50, 2026, null, 'CDI 01.02.05 bis', 'IS', 'Taux abattement CGA (50%)'),
('IS_PLAFOND_ABATTEMENT_CGA', 5000000, 2026, null, 'CDI 01.02.05 bis', 'IS', 'Plafond abattement CGA (5M)'),
('IS_TAUX_REDUCTION', 0.02, 2026, null, 'CDI 01.02.05 bis', 'IS', 'Taux reduction sur achats et charges salariales (2%)'),
('IS_TAUX_PLANCHER', 0.03, 2026, null, 'CDI 01.02.05', 'IS', 'Plancher IS (3% du CA brut)')
ON CONFLICT (code) DO NOTHING;
-- ============================================
-- IRSA - Impot sur les Revenus Salariaux
-- ============================================
INSERT INTO parametres_fiscaux (code, valeur, annee_debut, annee_fin, base_legale, secteur, description) VALUES
('IRSA_SEUIL_1', 350000, 2026, null, 'CDI 01.03.16', 'IRSA', 'Seuil tranche 1'),
('IRSA_SEUIL_2', 400000, 2026, null, 'CDI 01.03.16', 'IRSA', 'Seuil tranche 2'),
('IRSA_SEUIL_3', 500000, 2026, null, 'CDI 01.03.16', 'IRSA', 'Seuil tranche 3'),
('IRSA_SEUIL_4', 600000, 2026, null, 'CDI 01.03.16', 'IRSA', 'Seuil tranche 4'),
('IRSA_TAUX_0', 0.00, 2026, null, 'CDI 01.03.16', 'IRSA', 'Taux tranche 1 (<= 350K)'),
('IRSA_TAUX_1', 0.05, 2026, null, 'CDI 01.03.16', 'IRSA', 'Taux tranche 2 (350K-400K)'),
('IRSA_TAUX_2', 0.10, 2026, null, 'CDI 01.03.16', 'IRSA', 'Taux tranche 3 (400K-500K)'),
('IRSA_TAUX_3', 0.15, 2026, null, 'CDI 01.03.16', 'IRSA', 'Taux tranche 4 (500K-600K)'),
('IRSA_TAUX_4', 0.20, 2026, null, 'CDI 01.03.16', 'IRSA', 'Taux tranche 5 (> 600K)'),
('IRSA_MINIMUM', 3000, 2026, null, 'CDI 01.03.16', 'IRSA', 'Minimum IRSA'),
('IRSA_REDUCTION_PAR_PERSONNE', 2000, 2026, null, 'CDI 01.03.19', 'IRSA', 'Reduction par personne a charge')
ON CONFLICT (code) DO NOTHING;
-- ============================================
-- TVA - Taxe sur la Valeur Ajoutee (CDI 06.01.12, 06.01.04, 06.01.16)
-- ============================================
INSERT INTO parametres_fiscaux (code, valeur, annee_debut, annee_fin, base_legale, secteur, description) VALUES
('TVA_TAUX_NORMAL', 0.20, 2026, null, 'CDI 06.01.12', 'TVA', 'Taux normal TVA (20%)'),
('TVA_TAUX_EXPORT', 0.00, 2026, null, 'CDI 06.01.12', 'TVA', 'Taux TVA pour exportations (0%)'),
('TVA_SEUIL_ASSUJETTISSEMENT', 400000000, 2026, null, 'CDI 06.01.04', 'TVA', 'Seuil d''assujettissement TVA (400M)'),
('TVA_ECHEANCE_JOUR', 15, 2026, null, 'CDI 06.01.16', 'TVA', 'Jour echeance declarative TVA')
ON CONFLICT (code) DO NOTHING;
-- ============================================
-- Acomptes (IR et IS)
-- ============================================
INSERT INTO parametres_fiscaux (code, valeur, annee_debut, annee_fin, base_legale, secteur, description) VALUES
('ACOMPTE_TAUX_PROVISIONNEL', 0.50, 2026, null, 'Document IR 2026', 'ACOMPTE', 'Taux acompte provisionnel (50% de l''impot N-1)'),
('ACOMPTE_IS_AGRICOLE_ELEVEUR', 16000, 2026, null, 'Document IS 2026', 'ACOMPTE', 'Acompte debut activite IS - AGRICOLE, ELEVEUR'),
('ACOMPTE_IS_ARTISAN', 50000, 2026, null, 'Document IS 2026', 'ACOMPTE', 'Acompte debut activite IS - ARTISAN, GARGOTIER, PETIT_COMMERCANT'),
('ACOMPTE_IS_COMMERCANT', 100000, 2026, null, 'Document IS 2026', 'ACOMPTE', 'Acompte debut activite IS - COMMERCANT, HOTELIER, PRESTATAIRE'),
('ACOMPTE_IS_PROFESSION_LIBERALE', 150000, 2026, null, 'Document IS 2026', 'ACOMPTE', 'Acompte debut activite IS - PROFESSION_LIBERALE, ACTIVITES_MULTIPLES'),
('ACOMPTE_IR_INDUSTRIE_ETC', 500000, 2026, null, 'Document IR 2026', 'ACOMPTE', 'Acompte debut activite IR - INDUSTRIE, ARTISANAT, AGRICULTURE, TOURISME'),
('ACOMPTE_IR_AUTRES', 1000000, 2026, null, 'Document IR 2026', 'ACOMPTE', 'Acompte debut activite IR - autres categories'),
('ACOMPTE_IR_ECHEANCE_1', 31, 2026, null, 'Document IR 2026', 'ACOMPTE', 'Jour echeance 1er acompte IR (31 mars)'),
('ACOMPTE_IR_ECHEANCE_1_MOIS', 3, 2026, null, 'Document IR 2026', 'ACOMPTE', 'Mois echeance 1er acompte IR (mars)'),
('ACOMPTE_IR_ECHEANCE_2', 30, 2026, null, 'Document IR 2026', 'ACOMPTE', 'Jour echeance 2eme acompte IR (30 juin)'),
('ACOMPTE_IR_ECHEANCE_2_MOIS', 6, 2026, null, 'Document IR 2026', 'ACOMPTE', 'Mois echeance 2eme acompte IR (juin)')
ON CONFLICT (code) DO NOTHING;
-- ============================================
-- ECHANCES
-- ============================================
INSERT INTO parametres_fiscaux (code, valeur, annee_debut, annee_fin, base_legale, secteur, description) VALUES
('ECHEANCE_IR_EXERCICE_CIVIL_JOUR', 15, 2026, null, 'CPF I-11', 'ECHEANCE', 'Jour echeance IR - exercice civil (15 mai)'),
('ECHEANCE_IR_EXERCICE_CIVIL_MOIS', 5, 2026, null, 'CPF I-11', 'ECHEANCE', 'Mois echeance IR - exercice civil (mai)'),
('ECHEANCE_IR_CLOTURE_30_JUIN_JOUR', 15, 2026, null, 'CPF I-11', 'ECHEANCE', 'Jour echeance IR - cloture 30 juin (15 novembre)'),
('ECHEANCE_IR_CLOTURE_30_JUIN_MOIS', 11, 2026, null, 'CPF I-11', 'ECHEANCE', 'Mois echeance IR - cloture 30 juin (novembre)'),
('ECHEANCE_IS_JOUR', 31, 2026, null, 'CPF I-17', 'ECHEANCE', 'Jour echeance IS (31 mars)'),
('ECHEANCE_IS_MOIS', 3, 2026, null, 'CPF I-17', 'ECHEANCE', 'Mois echeance IS (mars)'),
('ECHEANCE_IRSA_JOUR', 15, 2026, null, 'CPF I-21', 'ECHEANCE', 'Jour echeance IRSA (15 du mois)'),
('ECHEANCE_IRSA_DELAI', 15, 2026, null, 'CPF I-21', 'ECHEANCE', 'Delai echeance IRSA (15 jours)')
ON CONFLICT (code) DO NOTHING;
-- ============================================
-- IMP - Impot sur les Marches Publics (CDI 01.01.51, 01.01.15)
-- ============================================
INSERT INTO parametres_fiscaux (code, valeur, annee_debut, annee_fin, base_legale, secteur, description) VALUES
('IMP_TAUX', 0.08, 2026, null, 'CDI 01.01.51', 'IMP', 'Taux IMP (8%)'),
('IMP_TAUX_ACOMPTE', 0.005, 2026, null, 'CDI 01.01.15', 'IMP', 'Acompte IMP aux formalites constitutives (5 pour mille)')
ON CONFLICT (code) DO NOTHING;
-- ============================================
-- IPVI - Impot sur les Plus-Values Immobilieres (CDI 01.05)
-- ============================================
INSERT INTO parametres_fiscaux (code, valeur, annee_debut, annee_fin, base_legale, secteur, description) VALUES
('IPVI_TAUX_PRIX_REVIENT', 0.75, 2026, null, 'CDI 01.05', 'IPVI', 'Prix de revient forfaitaire (75% du prix de vente)'),
('IPVI_TAUX', 0.20, 2026, null, 'CDI 01.05', 'IPVI', 'Taux IPVI (20%)')
ON CONFLICT (code) DO NOTHING;
-- ============================================
-- IRCM - Impot sur les Revenus des Collectivites (CDI Titre IRCM)
-- ============================================
INSERT INTO parametres_fiscaux (code, valeur, annee_debut, annee_fin, base_legale, secteur, description) VALUES
('IRCM_TAUX_NORMAL', 0.20, 2026, null, 'CDI Titre IRCM', 'IRCM', 'Taux normal IRCM (20%)'),
('IRCM_TAUX_COOPERATIVE', 0.10, 2026, null, 'CDI Titre IRCM', 'IRCM', 'Taux reduit pour cooperatives (10%)')
ON CONFLICT (code) DO NOTHING;
-- ============================================
-- REGIMES
-- Les seuils de regime IR (20 M) et IS synthetique (200 M) sont en conflit
-- entre sources (CDI vs documents 2026) : ils ne sont PAS seeds comme regles.
-- Seul le seuil TVA (400 M) est confirme (CDI 01.01.13 / 06.01.04).
-- Un avertissement ne peut etre produit que par un parametre marque « À CONFIRMER »
-- ajoute explicitement par un administrateur.
-- ============================================
INSERT INTO parametres_fiscaux (code, valeur, annee_debut, annee_fin, base_legale, secteur, description) VALUES
('REGIME_TVA_SEUIL', 400000000, 2026, null, 'CDI 06.01.04', 'REGIME', 'Seuil TVA (400M)')
ON CONFLICT (code) DO NOTHING;