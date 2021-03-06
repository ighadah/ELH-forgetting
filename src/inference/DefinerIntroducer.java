package inference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.*;

import BackTrack.BackTrack;
import checkexistence.EChecker;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.apibinding.OWLManager;
import checkfrequency.FChecker;
import concepts.AtomicConcept;
import connectives.And;
import connectives.Exists;
import connectives.Inclusion;
import formula.Formula;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import roles.AtomicRole;
import org.semanticweb.owlapi.apibinding.OWLManager;

public class DefinerIntroducer {

	public DefinerIntroducer() {

	}

	public static Map<Formula, AtomicConcept> definer_left_map = new HashMap<>();
	public static Map<Formula, AtomicConcept> definer_right_map = new HashMap<>();
	public static Set<OWLEntity> owldefiner_set = new LinkedHashSet<>();
	public static Set<AtomicConcept> definer_set = new LinkedHashSet<>();

	public List<Formula> removeCyclicDefinition(AtomicConcept concept,List<Formula> beforeIntroDefiners){
		FChecker fc = new FChecker();
		List<Formula> ans = new ArrayList<>();
		for(Formula formula : beforeIntroDefiners){
			if(fc.negative(concept,formula) > 0 && fc.positive(concept,formula) > 0){

			}
			else{
				ans.add(formula);
			}
		}
		return ans;
	}
	public List<Formula> introduceDefiners(AtomicConcept concept, List<Formula> input_list)
			throws Exception {

		List<Formula> output_list = new ArrayList<>();

		for (Formula formula : input_list) {

			Formula formulaClone = formula.clone();
			Formula outputFormula = formula.clone();

			List<Formula> re = introduceDefiners(concept, formulaClone);

			output_list.addAll(re);
			if(re.size() <= 1) continue;
			/*
			for(Formula temp : re){

				if(temp.toString().contains("Definer")){
					BackTrack.addFatherHash(temp,outputFormula,1);
				}
			}

			 */
			System.out.println("original formla : " );
			System.out.println(outputFormula+" "+concept);
			System.out.println("after introduced :");
			for(Formula temp : re){
				System.out.println(temp);

			}
			System.out.println("--------------------" );

		}

		return output_list;
	}

	public static void main(String[] args) {
		AtomicConcept a = new AtomicConcept("A");
		AtomicConcept b = new AtomicConcept("B");
		AtomicConcept c = new AtomicConcept("C");
		AtomicConcept d = new AtomicConcept("D");
		AtomicRole r = new AtomicRole("r");
		AtomicRole s = new AtomicRole("s");

		DefinerIntroducer di = new DefinerIntroducer();

		Formula temp = null;

		Set<Formula> andset2 = new LinkedHashSet<>();
		andset2.add(a);andset2.add(b);
		And and2 = new And(andset2); // a and b


		temp = new Exists(r,and2);

		Set<Formula> andset = new LinkedHashSet<>();
		andset.add(a);andset.add(temp);andset.add(new Exists(r,a));
		And and3 = new And(andset);
		Inclusion inc3 = new Inclusion(new Exists(r,and3),c);
		System.out.println(inc3);
		List<Formula> result2 = di.introduceDefiners(a, inc3);
		System.out.println("result2 = " + result2);
		System.out.println("result2 = " + inc3);


	}

	public List<Formula> introduceDefiners(AtomicConcept concept, Formula formula) {

		List<Formula> output_list = new ArrayList<>();
		EChecker ec = new EChecker();
		FChecker fc = new FChecker();

		Formula subsumee = formula.getSubFormulas().get(0);
		Formula subsumer = formula.getSubFormulas().get(1);

		int A_subsumee = fc.positive(concept, subsumee);
		int A_subsumer = fc.positive(concept, subsumer);

		if (subsumee.equals(subsumer)) {

		} else if (subsumee instanceof And && subsumee.getSubformulae().contains(subsumer)) {


		} else if (A_subsumee == 0 && A_subsumer == 0) {
			output_list.add(formula);

		} else if (A_subsumee == 1 && A_subsumer == 0) {

			if (subsumee instanceof Exists) {

				Formula filler = subsumee.getSubFormulas().get(1);

				if (filler instanceof Exists ||
						(filler instanceof And && !filler.getSubformulae().contains(concept))) {

					if (definer_right_map.get(filler) == null) {
						AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
						AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
						definer_set.add(definer);
						// owldefiner_set.add(bc.getClassfromConcept(definer));
						definer_right_map.put(filler, definer);
						subsumee.getSubFormulas().set(1, definer);
						output_list.add(formula);
						output_list.addAll(introduceDefiners(concept, new Inclusion(filler, definer)));

					} else {
						AtomicConcept definer = definer_right_map.get(filler);
						subsumee.getSubFormulas().set(1, definer);
						output_list.add(formula);
					}

				} else {
					output_list.add(formula);
				}

			} else if (subsumee instanceof And) {

				Set<Formula> conjunct_set = subsumee.getSubformulae();

				for (Formula conjunct : conjunct_set) {

					if (ec.isPresent(concept, conjunct) && conjunct instanceof Exists) {

						Formula filler = conjunct.getSubFormulas().get(1);
						// B and exists r.exists s.A in C
						if (filler instanceof Exists ||
								(filler instanceof And && !filler.getSubformulae().contains(concept))) {

							if (definer_right_map.get(filler) == null) {
								AtomicConcept definer = new AtomicConcept(
										"Definer" + AtomicConcept.getDefiner_index());
								AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
								definer_set.add(definer);
								// owldefiner_set.add(bc.getClassfromConcept(definer));
								definer_right_map.put(filler, definer);
								conjunct.getSubFormulas().set(1, definer);
								output_list.add(formula);
								output_list.addAll(introduceDefiners(concept, new Inclusion(filler, definer)));
								break;

							} else {
								AtomicConcept definer = definer_right_map.get(filler);
								conjunct.getSubFormulas().set(1, definer);
								output_list.add(formula);
								break;
							}
							// B and exists r.(C and exists s.A)
						} else {
							output_list.add(formula);
							break;
						}

					} else if (conjunct.equals(concept)) {
						output_list.add(formula);
						break;
					}
				}

			} else {
				output_list.add(formula);
			}

		} else if (A_subsumee > 1 && A_subsumer == 0) {

			if (subsumee instanceof Exists) {

				Formula filler = subsumee.getSubFormulas().get(1);

				if (definer_right_map.get(filler) == null) {
					AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
					AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
					definer_set.add(definer);
					// owldefiner_set.add(bc.getClassfromConcept(definer));
					definer_right_map.put(filler, definer);
					subsumee.getSubFormulas().set(1, definer);
					output_list.add(formula);
					output_list.addAll(introduceDefiners(concept, new Inclusion(filler, definer)));

				} else {
					AtomicConcept definer = definer_right_map.get(filler);
					subsumee.getSubFormulas().set(1, definer);
					output_list.add(formula);
				}

			} else if (subsumee instanceof And) {

				Set<Formula> conjunct_set = subsumee.getSubformulae();

				for (Formula conjunct : conjunct_set) {

					if (ec.isPresent(concept, conjunct) && conjunct instanceof Exists) {

						Formula filler = conjunct.getSubFormulas().get(1);

						if (definer_right_map.get(filler) == null) {
							AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
							AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
							definer_set.add(definer);
							// owldefiner_set.add(bc.getClassfromConcept(definer));
							definer_right_map.put(filler, definer);
							conjunct.getSubFormulas().set(1, definer);
							output_list.addAll(introduceDefiners(concept, formula));
							output_list.addAll(introduceDefiners(concept, new Inclusion(filler, definer)));
							break;

						} else {
							AtomicConcept definer = definer_right_map.get(filler);
							conjunct.getSubFormulas().set(1, definer);
							output_list.addAll(introduceDefiners(concept, formula));
							break;
						}
					}
				}
			}

		} else if (A_subsumee == 0 && A_subsumer == 1) {

			if (subsumer instanceof Exists) {

				Formula filler = subsumer.getSubFormulas().get(1);

				if (filler instanceof Exists) {

					if (definer_left_map.get(filler) == null) {
						AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
						AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
						definer_set.add(definer);
						// owldefiner_set.add(bc.getClassfromConcept(definer));
						definer_left_map.put(filler, definer);
						subsumer.getSubFormulas().set(1, definer);
						output_list.add(formula);
						output_list.addAll(introduceDefiners(concept, new Inclusion(definer, filler)));

					} else {
						AtomicConcept definer = definer_left_map.get(filler);
						subsumer.getSubFormulas().set(1, definer);
						output_list.add(formula);
					}

				} else if (filler instanceof And && !filler.getSubformulae().contains(concept)) {

					if (definer_left_map.get(filler) == null) {
						AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
						AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
						definer_set.add(definer);
						// owldefiner_set.add(bc.getClassfromConcept(definer));
						definer_left_map.put(filler, definer);
						subsumer.getSubFormulas().set(1, definer);
						output_list.add(formula);
						Set<Formula> conjunct_set = filler.getSubformulae();
						for (Formula conjunct : conjunct_set) {

							output_list.addAll(introduceDefiners(concept, new Inclusion(definer, conjunct)));
						}

					} else {
						AtomicConcept definer = definer_left_map.get(filler);
						subsumer.getSubFormulas().set(1, definer);
						output_list.add(formula);
					}

				} else {
					output_list.add(formula);
				}

			} else {
				output_list.add(formula);
			}

		} else if (A_subsumee == 1 && A_subsumer == 1) {

			if (subsumee instanceof Exists) {

				Formula filler = subsumee.getSubFormulas().get(1);

				if (filler instanceof Exists ||
						filler instanceof And && !filler.getSubformulae().contains(concept)) {

					if (definer_right_map.get(filler) == null) {
						AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
						AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
						definer_set.add(definer);
						// owldefiner_set.add(bc.getClassfromConcept(definer));
						definer_right_map.put(filler, definer);
						subsumee.getSubFormulas().set(1, definer);
						output_list.addAll(introduceDefiners(concept, formula));
						output_list.addAll(introduceDefiners(concept, new Inclusion(filler, definer)));

					} else {
						AtomicConcept definer = definer_right_map.get(filler);
						subsumee.getSubFormulas().set(1, definer);
						output_list.addAll(introduceDefiners(concept, formula));
					}

				} else {

					if (definer_left_map.get(subsumer) == null) {
						AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
						AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
						definer_set.add(definer);
						// owldefiner_set.add(bc.getClassfromConcept(definer));
						definer_left_map.put(subsumer, definer);
						formula.getSubFormulas().set(1, definer);
						output_list.add(formula);
						output_list.addAll(introduceDefiners(concept, new Inclusion(definer, subsumer)));

					} else {
						AtomicConcept definer = definer_left_map.get(subsumer);
						formula.getSubFormulas().set(1, definer);
						output_list.add(formula);
					}

				}

			} else if (subsumee instanceof And) {

				Set<Formula> conjunct_set = subsumee.getSubformulae();

				for (Formula conjunct : conjunct_set) {

					if (ec.isPresent(concept, conjunct) && conjunct instanceof Exists) {

						Formula filler = conjunct.getSubFormulas().get(1);

						if (filler instanceof Exists ||
								filler instanceof And && !filler.getSubformulae().contains(concept)) {

							if (definer_right_map.get(filler) == null) {
								AtomicConcept definer = new AtomicConcept(
										"Definer" + AtomicConcept.getDefiner_index());
								AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
								definer_set.add(definer);
								// owldefiner_set.add(bc.getClassfromConcept(definer));
								definer_right_map.put(filler, definer);
								conjunct.getSubFormulas().set(1, definer);
								output_list.addAll(introduceDefiners(concept, formula));
								output_list.addAll(introduceDefiners(concept, new Inclusion(filler, definer)));
								break;

							} else {
								AtomicConcept definer = definer_right_map.get(filler);
								conjunct.getSubFormulas().set(1, definer);
								output_list.addAll(introduceDefiners(concept, formula));
								break;
							}

						} else {

							if (definer_left_map.get(subsumer) == null) {
								AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
								AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
								definer_set.add(definer);
								// owldefiner_set.add(bc.getClassfromConcept(definer));
								definer_left_map.put(subsumer, definer);
								formula.getSubFormulas().set(1, definer);
								output_list.add(formula);
								output_list.addAll(introduceDefiners(concept, new Inclusion(definer, subsumer)));
								break;

							} else {
								AtomicConcept definer = definer_left_map.get(subsumer);
								formula.getSubFormulas().set(1, definer);
								output_list.add(formula);
								break;
							}

						}

					} else if (conjunct.equals(concept)) {

						if (definer_left_map.get(subsumer) == null) {
							AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
							AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
							definer_set.add(definer);
							// owldefiner_set.add(bc.getClassfromConcept(definer));
							definer_left_map.put(subsumer, definer);
							formula.getSubFormulas().set(1, definer);
							output_list.add(formula);
							output_list.addAll(introduceDefiners(concept, new Inclusion(definer, subsumer)));
							break;

						} else {
							AtomicConcept definer = definer_left_map.get(subsumer);
							formula.getSubFormulas().set(1, definer);
							output_list.add(formula);
							break;
						}
					}
				}

			} else {

				if (definer_left_map.get(subsumer) == null) {
					AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
					AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
					definer_set.add(definer);
					// owldefiner_set.add(bc.getClassfromConcept(definer));
					definer_left_map.put(subsumer, definer);
					formula.getSubFormulas().set(1, definer);
					output_list.add(formula);
					output_list.addAll(introduceDefiners(concept, new Inclusion(definer, subsumer)));

				} else {
					AtomicConcept definer = definer_left_map.get(subsumer);
					formula.getSubFormulas().set(1, definer);
					output_list.add(formula);
				}

			}
////////////////////////////////
		} else if (A_subsumee > 1 && A_subsumer == 1) {

			if (subsumee instanceof Exists) {

				Formula filler = subsumee.getSubFormulas().get(1);

				if (definer_right_map.get(filler) == null) {
					AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
					AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
					definer_set.add(definer);
					// owldefiner_set.add(bc.getClassfromConcept(definer));
					definer_right_map.put(filler, definer);
					subsumee.getSubFormulas().set(1, definer);
					output_list.addAll(introduceDefiners(concept, formula));
					output_list.addAll(introduceDefiners(concept, new Inclusion(filler, definer)));

				} else {
					AtomicConcept definer = definer_right_map.get(filler);
					subsumee.getSubFormulas().set(1, definer);
					output_list.addAll(introduceDefiners(concept, formula));
				}

			} else if (subsumee instanceof And) {

				Set<Formula> conjunct_set = subsumee.getSubformulae();

				for (Formula conjunct : conjunct_set) {

					if (ec.isPresent(concept, conjunct)) {

						Formula filler = conjunct.getSubFormulas().get(1);

						if (definer_right_map.get(filler) == null) {
							AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
							AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
							definer_set.add(definer);
							// owldefiner_set.add(bc.getClassfromConcept(definer));
							definer_right_map.put(filler, definer);
							conjunct.getSubFormulas().set(1, definer);
							output_list.addAll(introduceDefiners(concept, formula));
							output_list.addAll(introduceDefiners(concept, new Inclusion(filler, definer)));
							break;

						} else {
							AtomicConcept definer = definer_right_map.get(filler);
							conjunct.getSubFormulas().set(1, definer);
							output_list.addAll(introduceDefiners(concept, formula));
							break;
						}
					}
				}
			}

		} else if (A_subsumee == 0 && A_subsumer > 1) {

			if (subsumer instanceof Exists) {

				Formula filler = subsumer.getSubFormulas().get(1);

				if (filler instanceof Exists) {

					if (definer_left_map.get(filler) == null) {
						AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
						AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
						definer_set.add(definer);
						// owldefiner_set.add(bc.getClassfromConcept(definer));
						definer_left_map.put(filler, definer);
						subsumer.getSubFormulas().set(1, definer);
						output_list.add(formula);
						output_list.addAll(introduceDefiners(concept, new Inclusion(definer, filler)));

					} else {
						AtomicConcept definer = definer_left_map.get(filler);
						subsumer.getSubFormulas().set(1, definer);
						output_list.add(formula);
					}

				} else if (filler instanceof And) {

					if (definer_left_map.get(filler) == null) {
						AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
						AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
						definer_set.add(definer);
						// owldefiner_set.add(bc.getClassfromConcept(definer));
						definer_left_map.put(filler, definer);
						subsumer.getSubFormulas().set(1, definer);
						output_list.add(formula);
						Set<Formula> conjunct_set = filler.getSubformulae();
						for (Formula conjunct : conjunct_set) {
							output_list.addAll(introduceDefiners(concept, new Inclusion(definer, conjunct)));
						}

					} else {
						AtomicConcept definer = definer_left_map.get(filler);
						subsumer.getSubFormulas().set(1, definer);
						output_list.add(formula);
					}
				}
			}

		} else if (A_subsumee == 1 && A_subsumer > 1) {

			if (subsumee instanceof Exists) {

				Formula filler = subsumee.getSubFormulas().get(1);

				if (filler instanceof Exists ||
						filler instanceof And && !filler.getSubformulae().contains(concept)) {

					if (definer_right_map.get(filler) == null) {
						AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
						AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
						definer_set.add(definer);
						// owldefiner_set.add(bc.getClassfromConcept(definer));
						definer_right_map.put(filler, definer);
						subsumee.getSubFormulas().set(1, definer);
						output_list.addAll(introduceDefiners(concept, formula));
						output_list.addAll(introduceDefiners(concept, new Inclusion(filler, definer)));

					} else {
						AtomicConcept definer = definer_right_map.get(filler);
						subsumee.getSubFormulas().set(1, definer);
						output_list.addAll(introduceDefiners(concept, formula));
					}

				} else {

					if (definer_left_map.get(subsumer) == null) {
						AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
						AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
						definer_set.add(definer);
						// owldefiner_set.add(bc.getClassfromConcept(definer));
						definer_left_map.put(subsumer, definer);
						formula.getSubFormulas().set(1, definer);
						output_list.add(formula);
						output_list.addAll(introduceDefiners(concept, new Inclusion(definer, subsumer)));

					} else {
						AtomicConcept definer = definer_left_map.get(subsumer);
						formula.getSubFormulas().set(1, definer);
						output_list.add(formula);
					}

				}

			} else if (subsumee instanceof And) {

				Set<Formula> conjunct_set = subsumee.getSubformulae();

				for (Formula conjunct : conjunct_set) {

					if (ec.isPresent(concept, conjunct) && conjunct instanceof Exists) {

						Formula filler = conjunct.getSubFormulas().get(1);

						if (filler instanceof Exists ||
								filler instanceof And && !filler.getSubformulae().contains(concept)) {

							if (definer_right_map.get(filler) == null) {
								AtomicConcept definer = new AtomicConcept(
										"Definer" + AtomicConcept.getDefiner_index());
								AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
								definer_set.add(definer);
								// owldefiner_set.add(bc.getClassfromConcept(definer));
								definer_right_map.put(filler, definer);
								conjunct.getSubFormulas().set(1, definer);
								output_list.addAll(introduceDefiners(concept, formula));
								output_list.addAll(introduceDefiners(concept, new Inclusion(filler, definer)));
								break;

							} else {
								AtomicConcept definer = definer_right_map.get(filler);
								conjunct.getSubFormulas().set(1, definer);
								output_list.addAll(introduceDefiners(concept, formula));
								break;
							}

						} else {

							if (definer_left_map.get(subsumer) == null) {
								AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
								AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
								definer_set.add(definer);
								// owldefiner_set.add(bc.getClassfromConcept(definer));
								definer_left_map.put(subsumer, definer);
								formula.getSubFormulas().set(1, definer);
								output_list.add(formula);
								output_list.addAll(introduceDefiners(concept, new Inclusion(definer, subsumer)));
								break;

							} else {
								AtomicConcept definer = definer_left_map.get(subsumer);
								formula.getSubFormulas().set(1, definer);
								output_list.add(formula);
								break;
							}
						}

					} else if (conjunct.equals(concept)) {

						if (definer_left_map.get(subsumer) == null) {
							AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
							AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
							definer_set.add(definer);
							// owldefiner_set.add(bc.getClassfromConcept(definer));
							definer_left_map.put(subsumer, definer);
							formula.getSubFormulas().set(1, definer);
							output_list.add(formula);
							output_list.addAll(introduceDefiners(concept, new Inclusion(definer, subsumer)));
							break;

						} else {
							AtomicConcept definer = definer_left_map.get(subsumer);
							formula.getSubFormulas().set(1, definer);
							output_list.add(formula);
							break;
						}
					}
				}

			} else {

				if (definer_left_map.get(subsumer) == null) {
					AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
					AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
					definer_set.add(definer);
					// owldefiner_set.add(bc.getClassfromConcept(definer));
					definer_left_map.put(subsumer, definer);
					formula.getSubFormulas().set(1, definer);
					output_list.add(formula);
					output_list.addAll(introduceDefiners(concept, new Inclusion(definer, subsumer)));

				} else {
					AtomicConcept definer = definer_left_map.get(subsumer);
					formula.getSubFormulas().set(1, definer);
					output_list.add(formula);
				}

			}

		} else if (A_subsumee > 1 && A_subsumer > 1) {

			if (subsumee instanceof Exists) {

				Formula filler = subsumee.getSubFormulas().get(1);

				if (definer_right_map.get(filler) == null) {
					AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
					AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
					definer_set.add(definer);
					// owldefiner_set.add(bc.getClassfromConcept(definer));
					definer_right_map.put(filler, definer);
					subsumee.getSubFormulas().set(1, definer);
					output_list.addAll(introduceDefiners(concept, formula));
					output_list.addAll(introduceDefiners(concept, new Inclusion(filler, definer)));

				} else {
					AtomicConcept definer = definer_right_map.get(filler);
					subsumee.getSubFormulas().set(1, definer);
					output_list.addAll(introduceDefiners(concept, formula));
				}

			} else if (subsumee instanceof And) {

				Set<Formula> conjunct_set = subsumee.getSubformulae();

				for (Formula conjunct : conjunct_set) {

					if (ec.isPresent(concept, conjunct) && conjunct instanceof Exists) {

						Formula filler = conjunct.getSubFormulas().get(1);

						if (definer_right_map.get(filler) == null) {
							AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
							AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
							definer_set.add(definer);
							// owldefiner_set.add(bc.getClassfromConcept(definer));
							definer_right_map.put(filler, definer);
							conjunct.getSubFormulas().set(1, definer);
							output_list.addAll(introduceDefiners(concept, formula));
							output_list.addAll(introduceDefiners(concept, new Inclusion(filler, definer)));
							break;

						} else {
							AtomicConcept definer = definer_right_map.get(filler);
							conjunct.getSubFormulas().set(1, definer);
							output_list.addAll(introduceDefiners(concept, formula));
							break;
						}
					}
				}
			}

		} else {
			output_list.add(formula);
		}

		return output_list;
	}

















	public List<Formula> introduceDefiners(AtomicRole role, List<Formula> input_list)
			throws Exception {

		List<Formula> output_list = new ArrayList<>();


		for (Formula formula : input_list) {

			Formula formulaClone = formula.clone();
			Formula outputFormula = formula.clone();

			List<Formula> re = introduceDefiners(role, formulaClone);

			output_list.addAll(re);
			if(re.size() <= 1) continue;

/*
			for(Formula temp : re){

				if(temp.toString().contains("Definer")){
					BackTrack.addFatherHash(temp,outputFormula,1);
				}
			}

 */
			System.out.println("original formla and role: " );
			System.out.println(outputFormula+" "+role);
			System.out.println("after introduced :");
			for(Formula temp : re){
				System.out.println(temp);
				System.out.println(temp.getSubFormulas().get(0).getClass());
				System.out.println(temp.getSubFormulas().get(1).getClass());

			}
			System.out.println("--------------------" );

		}
		return output_list;


	}

	public List<Formula> introduceDefiners(AtomicRole role, Formula formula) {

		List<Formula> output_list = new ArrayList<>();
		EChecker ec = new EChecker();
		FChecker fc = new FChecker();

		Formula subsumee = formula.getSubFormulas().get(0);
		Formula subsumer = formula.getSubFormulas().get(1);

		int r_subsumee = fc.positive(role, subsumee);
		int r_subsumer = fc.positive(role, subsumer);


		if (subsumee.equals(subsumer)) {

		} else if (subsumee instanceof And && subsumee.getSubformulae().contains(subsumer)) {


		} else if (r_subsumee == 0 && r_subsumer == 0) {
			output_list.add(formula);

		} else if (r_subsumee == 1 && r_subsumer == 0) {

			if (subsumee instanceof Exists) {

				Formula relation = subsumee.getSubFormulas().get(0);
				Formula filler = subsumee.getSubFormulas().get(1);

				if (!relation.equals(role)) {

					if (definer_right_map.get(filler) == null) {
						AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
						AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
						definer_set.add(definer);
						// owldefiner_set.add(bc.getClassfromConcept(definer));
						definer_right_map.put(filler, definer);
						subsumee.getSubFormulas().set(1, definer);
						output_list.add(formula);
						output_list.addAll(introduceDefiners(role, new Inclusion(filler, definer)));

					} else {
						AtomicConcept definer = definer_right_map.get(filler);
						subsumee.getSubFormulas().set(1, definer);
						output_list.add(formula);
					}

				} else {
					output_list.add(formula);
				}

			} else if (subsumee instanceof And) {

				Set<Formula> conjunct_set = subsumee.getSubformulae();

				for (Formula conjunct : conjunct_set) {

					if (ec.isPresent(role, conjunct)) {

						Formula relation = conjunct.getSubFormulas().get(0);

						if (!relation.equals(role)) {

							Formula filler = conjunct.getSubFormulas().get(1);

							if (definer_right_map.get(filler) == null) {
								AtomicConcept definer = new AtomicConcept(
										"Definer" + AtomicConcept.getDefiner_index());
								AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
								definer_set.add(definer);
								// owldefiner_set.add(bc.getClassfromConcept(definer));
								definer_right_map.put(filler, definer);
								conjunct.getSubFormulas().set(1, definer);
								output_list.add(formula);
								output_list.addAll(introduceDefiners(role, new Inclusion(filler, definer)));
								break;

							} else {
								AtomicConcept definer = definer_right_map.get(filler);
								conjunct.getSubFormulas().set(1, definer);
								output_list.add(formula);
								break;
							}

						} else {
							output_list.add(formula);
							break;
						}
					}
				}

			} else {
				output_list.add(formula);
			}

		} else if (r_subsumee > 1 && r_subsumer == 0) {

			if (subsumee instanceof Exists) {

				Formula filler = subsumee.getSubFormulas().get(1);

				if (definer_right_map.get(filler) == null) {
					AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
					AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
					definer_set.add(definer);
					// owldefiner_set.add(bc.getClassfromConcept(definer));
					definer_right_map.put(filler, definer);
					subsumee.getSubFormulas().set(1, definer);
					output_list.add(formula);
					output_list.addAll(introduceDefiners(role, new Inclusion(filler, definer)));

				} else {
					AtomicConcept definer = definer_right_map.get(filler);
					subsumee.getSubFormulas().set(1, definer);
					output_list.add(formula);
				}

			} else if (subsumee instanceof And) {

				Set<Formula> conjunct_set = subsumee.getSubformulae();

				for (Formula conjunct : conjunct_set) {

					if (ec.isPresent(role, conjunct)) {

						Formula filler = conjunct.getSubFormulas().get(1);

						if (definer_right_map.get(filler) == null) {
							AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
							AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
							definer_set.add(definer);
							// owldefiner_set.add(bc.getClassfromConcept(definer));
							definer_right_map.put(filler, definer);
							conjunct.getSubFormulas().set(1, definer);
							output_list.addAll(introduceDefiners(role, formula));
							output_list.addAll(introduceDefiners(role, new Inclusion(filler, definer)));
							break;

						} else {
							AtomicConcept definer = definer_right_map.get(filler);
							conjunct.getSubFormulas().set(1, definer);
							output_list.addAll(introduceDefiners(role, formula));
							break;
						}
					}
				}
			}

		} else if (r_subsumee == 0 && r_subsumer == 1) {

			if (subsumer instanceof Exists) {

				Formula relation = subsumer.getSubFormulas().get(0);
				Formula filler = subsumer.getSubFormulas().get(1);

				if (!relation.equals(role)) {

					if (definer_left_map.get(filler) == null) {
						AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
						AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
						definer_set.add(definer);
						// owldefiner_set.add(bc.getClassfromConcept(definer));
						definer_left_map.put(filler, definer);
						subsumer.getSubFormulas().set(1, definer);
						output_list.add(formula);
						if (filler instanceof And) {
							Set<Formula> filler_conjunct_set = filler.getSubformulae();
							for (Formula conjunct : filler_conjunct_set) {
								output_list.addAll(introduceDefiners(role, new Inclusion(definer, conjunct)));
							}

						} else {
							output_list.addAll(introduceDefiners(role, new Inclusion(definer, filler)));
						}

					} else {
						AtomicConcept definer = definer_left_map.get(filler);
						subsumer.getSubFormulas().set(1, definer);
						output_list.add(formula);
					}

				} else {
					output_list.add(formula);
				}

			} else {
				output_list.add(formula);
			}

		} else if (r_subsumee == 1 && r_subsumer == 1) {

			if (subsumee instanceof Exists) {

				Formula relation = subsumee.getSubFormulas().get(0);
				Formula filler = subsumee.getSubFormulas().get(1);

				if (!relation.equals(role)) {

					if (definer_right_map.get(filler) == null) {
						AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
						AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
						definer_set.add(definer);
						// owldefiner_set.add(bc.getClassfromConcept(definer));
						definer_right_map.put(filler, definer);
						subsumee.getSubFormulas().set(1, definer);
						output_list.addAll(introduceDefiners(role, formula));
						output_list.addAll(introduceDefiners(role, new Inclusion(filler, definer)));

					} else {
						AtomicConcept definer = definer_right_map.get(filler);
						subsumee.getSubFormulas().set(1, definer);
						output_list.addAll(introduceDefiners(role, formula));
					}

				} else {

					if (definer_left_map.get(subsumer) == null) {
						AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
						AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
						definer_set.add(definer);
						// owldefiner_set.add(bc.getClassfromConcept(definer));
						definer_left_map.put(subsumer, definer);
						formula.getSubFormulas().set(1, definer);
						output_list.add(formula);
						output_list.addAll(introduceDefiners(role, new Inclusion(definer, subsumer)));

					} else {
						AtomicConcept definer = definer_left_map.get(subsumer);
						formula.getSubFormulas().set(1, definer);
						output_list.add(formula);
					}
				}

			} else if (subsumee instanceof And) {

				Set<Formula> conjunct_set = subsumee.getSubformulae();

				for (Formula conjunct : conjunct_set) {

					if (ec.isPresent(role, conjunct)) {

						Formula relation = conjunct.getSubFormulas().get(0);

						if (!relation.equals(role)) {

							Formula filler = conjunct.getSubFormulas().get(1);

							if (definer_right_map.get(filler) == null) {
								AtomicConcept definer = new AtomicConcept(
										"Definer" + AtomicConcept.getDefiner_index());
								AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
								definer_set.add(definer);
								// owldefiner_set.add(bc.getClassfromConcept(definer));
								definer_right_map.put(filler, definer);
								conjunct.getSubFormulas().set(1, definer);
								output_list.addAll(introduceDefiners(role, formula));
								output_list.addAll(introduceDefiners(role, new Inclusion(filler, definer)));
								break;

							} else {
								AtomicConcept definer = definer_right_map.get(filler);
								conjunct.getSubFormulas().set(1, definer);
								output_list.addAll(introduceDefiners(role, formula));
								break;
							}

						} else {

							if (definer_left_map.get(subsumer) == null) {
								AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
								AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
								definer_set.add(definer);
								// owldefiner_set.add(bc.getClassfromConcept(definer));
								definer_left_map.put(subsumer, definer);
								formula.getSubFormulas().set(1, definer);
								output_list.add(formula);
								output_list.addAll(introduceDefiners(role, new Inclusion(definer, subsumer)));
								break;

							} else {
								AtomicConcept definer = definer_left_map.get(subsumer);
								formula.getSubFormulas().set(1, definer);
								output_list.add(formula);
								break;
							}
						}
					}
				}

			} else {
				output_list.add(formula);
			}

		} else if (r_subsumee > 1 && r_subsumer == 1) {

			if (subsumee instanceof Exists) {

				Formula filler = subsumee.getSubFormulas().get(1);

				if (definer_right_map.get(filler) == null) {
					AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
					AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
					definer_set.add(definer);
					// owldefiner_set.add(bc.getClassfromConcept(definer));
					definer_right_map.put(filler, definer);
					subsumee.getSubFormulas().set(1, definer);
					output_list.addAll(introduceDefiners(role, formula));
					output_list.addAll(introduceDefiners(role, new Inclusion(filler, definer)));

				} else {
					AtomicConcept definer = definer_right_map.get(filler);
					subsumee.getSubFormulas().set(1, definer);
					output_list.addAll(introduceDefiners(role, formula));
				}

			} else if (subsumee instanceof And) {

				Set<Formula> conjunct_set = subsumee.getSubformulae();

				for (Formula conjunct : conjunct_set) {

					if (ec.isPresent(role, conjunct)) {

						Formula filler = conjunct.getSubFormulas().get(1);

						if (definer_right_map.get(filler) == null) {
							AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
							AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
							definer_set.add(definer);
							// owldefiner_set.add(bc.getClassfromConcept(definer));
							definer_right_map.put(filler, definer);
							conjunct.getSubFormulas().set(1, definer);
							output_list.addAll(introduceDefiners(role, formula));
							output_list.addAll(introduceDefiners(role, new Inclusion(filler, definer)));
							break;

						} else {
							AtomicConcept definer = definer_right_map.get(filler);
							conjunct.getSubFormulas().set(1, definer);
							output_list.addAll(introduceDefiners(role, formula));
							break;
						}
					}
				}
			}

		} else if (r_subsumee == 0 && r_subsumer > 1) {

			if (subsumer instanceof Exists) {

				Formula filler = subsumer.getSubFormulas().get(1);

				if (definer_left_map.get(filler) == null) {
					AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
					AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
					definer_set.add(definer);
					// owldefiner_set.add(bc.getClassfromConcept(definer));
					definer_left_map.put(filler, definer);
					subsumer.getSubFormulas().set(1, definer);
					output_list.add(formula);
					if (filler instanceof And) {
						Set<Formula> filler_conjunct_set = filler.getSubformulae();
						for (Formula conjunct : filler_conjunct_set) {
							output_list.addAll(introduceDefiners(role, new Inclusion(definer, conjunct)));
						}

					} else {
						output_list.addAll(introduceDefiners(role, new Inclusion(definer, filler)));
					}

				} else {
					AtomicConcept definer = definer_left_map.get(filler);
					subsumer.getSubFormulas().set(1, definer);
					output_list.add(formula);
				}
			}

		} else if (r_subsumee == 1 && r_subsumer > 1) {

			if (subsumee instanceof Exists) {

				Formula relation = subsumee.getSubFormulas().get(0);
				Formula filler = subsumee.getSubFormulas().get(1);

				if (!relation.equals(role)) {

					if (definer_right_map.get(filler) == null) {
						AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
						AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
						definer_set.add(definer);
						// owldefiner_set.add(bc.getClassfromConcept(definer));
						definer_right_map.put(filler, definer);
						subsumee.getSubFormulas().set(1, definer);
						output_list.addAll(introduceDefiners(role, formula));
						output_list.addAll(introduceDefiners(role, new Inclusion(filler, definer)));

					} else {
						AtomicConcept definer = definer_right_map.get(filler);
						subsumee.getSubFormulas().set(1, definer);
						output_list.addAll(introduceDefiners(role, formula));
					}

				} else {

					if (definer_left_map.get(subsumer) == null) {
						AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
						AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
						definer_set.add(definer);
						// owldefiner_set.add(bc.getClassfromConcept(definer));
						definer_left_map.put(subsumer, definer);
						formula.getSubFormulas().set(1, definer);
						output_list.add(formula);
						output_list.addAll(introduceDefiners(role, new Inclusion(definer, subsumer)));

					} else {
						AtomicConcept definer = definer_left_map.get(subsumer);
						formula.getSubFormulas().set(1, definer);
						output_list.add(formula);
					}
				}

			} else if (subsumee instanceof And) {

				Set<Formula> conjunct_set = subsumee.getSubformulae();

				for (Formula conjunct : conjunct_set) {

					if (ec.isPresent(role, conjunct)) {

						Formula relation = conjunct.getSubFormulas().get(0);

						if (!relation.equals(role)) {

							Formula filler = conjunct.getSubFormulas().get(1);

							if (definer_right_map.get(filler) == null) {
								AtomicConcept definer = new AtomicConcept(
										"Definer" + AtomicConcept.getDefiner_index());
								AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
								definer_set.add(definer);
								// owldefiner_set.add(bc.getClassfromConcept(definer));
								definer_right_map.put(filler, definer);
								conjunct.getSubFormulas().set(1, definer);
								output_list.addAll(introduceDefiners(role, formula));
								output_list.addAll(introduceDefiners(role, new Inclusion(filler, definer)));
								break;

							} else {
								AtomicConcept definer = definer_right_map.get(filler);
								conjunct.getSubFormulas().set(1, definer);
								output_list.addAll(introduceDefiners(role, formula));
								break;
							}

						} else {

							if (definer_left_map.get(subsumer) == null) {
								AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
								AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
								definer_set.add(definer);
								// owldefiner_set.add(bc.getClassfromConcept(definer));
								definer_left_map.put(subsumer, definer);
								formula.getSubFormulas().set(1, definer);
								output_list.add(formula);
								output_list.addAll(introduceDefiners(role, new Inclusion(definer, subsumer)));
								break;

							} else {
								AtomicConcept definer = definer_left_map.get(subsumer);
								formula.getSubFormulas().set(1, definer);
								output_list.add(formula);
								break;
							}
						}
					}
				}
			}

		} else if (r_subsumee > 1 && r_subsumer > 1) {

			if (subsumee instanceof Exists) {

				Formula filler = subsumee.getSubFormulas().get(1);

				if (definer_right_map.get(filler) == null) {
					AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
					AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
					definer_set.add(definer);
					// owldefiner_set.add(bc.getClassfromConcept(definer));
					definer_right_map.put(filler, definer);
					subsumee.getSubFormulas().set(1, definer);
					output_list.addAll(introduceDefiners(role, formula));
					output_list.addAll(introduceDefiners(role, new Inclusion(filler, definer)));

				} else {
					AtomicConcept definer = definer_right_map.get(filler);
					subsumee.getSubFormulas().set(1, definer);
					output_list.addAll(introduceDefiners(role, formula));
				}

			} else if (subsumee instanceof And) {

				Set<Formula> conjunct_set = subsumee.getSubformulae();

				for (Formula conjunct : conjunct_set) {

					if (ec.isPresent(role, conjunct)) {

						Formula filler = conjunct.getSubFormulas().get(1);

						if (definer_right_map.get(filler) == null) {
							AtomicConcept definer = new AtomicConcept("Definer" + AtomicConcept.getDefiner_index());
							AtomicConcept.setDefiner_index(AtomicConcept.getDefiner_index() + 1);
							definer_set.add(definer);
							// owldefiner_set.add(bc.getClassfromConcept(definer));
							definer_right_map.put(filler, definer);
							conjunct.getSubFormulas().set(1, definer);
							output_list.addAll(introduceDefiners(role, formula));
							output_list.addAll(introduceDefiners(role, new Inclusion(filler, definer)));
							break;

						} else {
							AtomicConcept definer = definer_right_map.get(filler);
							conjunct.getSubFormulas().set(1, definer);
							output_list.addAll(introduceDefiners(role, formula));
							break;
						}
					}
				}
			}

		} else {
			output_list.add(formula);
		}

		return output_list;
	}

}
