package simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import explicit.GSMPEvent;
import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.Command;
import parser.ast.DistributionList;
import parser.ast.Event;
import parser.ast.Expression;
import parser.ast.ExpressionIdent;
import parser.ast.ExpressionLiteral;
import parser.ast.LabelList;
import parser.ast.ModulesFile;
import parser.ast.RewardStruct;
import parser.type.Type;
import parser.type.TypeDistribution;
import parser.type.TypeDistributionExponential;
import parser.type.TypeDouble;
import prism.DefaultModelGenerator;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;

public class ModulesFileModelGenerator extends DefaultModelGenerator
{
	// Parent PrismComponent (logs, settings etc.)
	protected PrismComponent parent;
	
	// PRISM model info
	/** The original modules file (might have unresolved constants) */
	private ModulesFile originalModulesFile;
	/** The modules file used for generating (has no unresolved constants after {@code initialise}) */
	private ModulesFile modulesFile;
	private ModelType modelType;
	private Values mfConstants;
	private VarList varList;
	private LabelList labelList;
	private List<String> labelNames;
	
	// Model exploration info
	
	// State currently being explored
	private State exploreState;
	// Updater object for model
	protected Updater updater;
	// List of currently available transitions
	protected TransitionList transitionList;
	// Has the transition list been built? 
	protected boolean transitionListBuilt;
	
	/**
	 * Build a ModulesFileModelGenerator for a particular PRISM model, represented by a ModuleFile instance.
	 * @param modulesFile The PRISM model
	 */
	public ModulesFileModelGenerator(ModulesFile modulesFile) throws PrismException
	{
		this(modulesFile, null);
	}
	
	/**
	 * Build a ModulesFileModelGenerator for a particular PRISM model, represented by a ModuleFile instance.
	 * @param modulesFile The PRISM model
	 */
	public ModulesFileModelGenerator(ModulesFile modulesFile, PrismComponent parent) throws PrismException
	{
		this.parent = parent;
		
		// No support for PTAs yet
		if (modulesFile.getModelType() == ModelType.PTA) {
			throw new PrismException("Sorry - the simulator does not currently support PTAs");
		}
		// No support for system...endsystem yet
		if (modulesFile.getSystemDefn() != null) {
			throw new PrismException("Sorry - the simulator does not currently handle the system...endsystem construct");
		}
		
		// Store basic model info
		this.modulesFile = modulesFile;
		this.originalModulesFile = modulesFile;
		modelType = modulesFile.getModelType();
		
		// If there are no constants to define, go ahead and initialise;
		// Otherwise, setSomeUndefinedConstants needs to be called when the values are available  
		mfConstants = modulesFile.getConstantValues();
		if (mfConstants != null) {
			initialise();
		}
	}
	
	/**
	 * (Re-)Initialise the class ready for model exploration
	 * (can only be done once any constants needed have been provided)
	 */
	private void initialise() throws PrismLangException
	{
		// Evaluate constants on (a copy) of the modules file, insert constant values and optimize arithmetic expressions
		modulesFile = (ModulesFile) modulesFile.deepCopy().replaceConstants(mfConstants).simplify();

		// Get info
		varList = modulesFile.createVarList();
		labelList = modulesFile.getLabelList();
		labelNames = labelList.getLabelNames();
		
		// Create data structures for exploring model
		updater = new Updater(modulesFile, varList, parent);
		transitionList = new TransitionList();
		transitionListBuilt = false;
	}
	
	// Methods for ModelInfo interface
	
	@Override
	public ModelType getModelType()
	{
		return modelType;
	}
	
	@Override
	public void setSomeUndefinedConstants(Values someValues) throws PrismException
	{
		// We start again with a copy of the original modules file
		// and set the constants in the copy.
		// As {@code initialise()} can replace references to constants
		// with the concrete values in modulesFile, this ensures that we
		// start again at a place where references to constants have not
		// yet been replaced.
		modulesFile = (ModulesFile) originalModulesFile.deepCopy();
		modulesFile.setSomeUndefinedConstants(someValues);
		mfConstants = modulesFile.getConstantValues();
		initialise();
	}
	
	@Override
	public Values getConstantValues()
	{
		return mfConstants;
	}
	
	@Override
	public boolean containsUnboundedVariables()
	{
		return modulesFile.containsUnboundedVariables();
	}
	
	@Override
	public int getNumVars()
	{
		return modulesFile.getNumVars();
	}
	
	@Override
	public List<String> getVarNames()
	{
		return modulesFile.getVarNames();
	}

	@Override
	public List<Type> getVarTypes()
	{
		return modulesFile.getVarTypes();
	}

	@Override
	public int getNumLabels()
	{
		return labelList.size();	
	}

	@Override
	public List<String> getLabelNames()
	{
		return labelNames;
	}
	
	@Override
	public String getLabelName(int i) throws PrismException
	{
		return labelList.getLabelName(i);
	}
	
	@Override
	public int getLabelIndex(String label)
	{
		return labelList.getLabelIndex(label);
	}
	
	@Override
	public int getNumRewardStructs()
	{
		return modulesFile.getNumRewardStructs();
	}
	
	@Override
	public List<String> getRewardStructNames()
	{
		return modulesFile.getRewardStructNames();
	}
	
	@Override
	public int getRewardStructIndex(String name)
	{
		return modulesFile.getRewardStructIndex(name);
	}
	
	@Override
	public RewardStruct getRewardStruct(int i)
	{
		return modulesFile.getRewardStruct(i);
	}

	// Methods for ModelGenerator interface
	
	@Override
	public boolean hasSingleInitialState() throws PrismException
	{
		return modulesFile.getInitialStates() == null;
	}
	
	@Override
	public State getInitialState() throws PrismException
	{
		if (modulesFile.getInitialStates() == null) {
			return modulesFile.getDefaultInitialState();
		} else {
			// Inefficient but probably won't be called
			return getInitialStates().get(0);
		}
	}
	
	@Override
	public List<State> getInitialStates() throws PrismException
	{
		List<State> initStates = new ArrayList<State>();
		// Easy (normal) case: just one initial state
		if (modulesFile.getInitialStates() == null) {
			State state = modulesFile.getDefaultInitialState();
			initStates.add(state);
		}
		// Otherwise, there may be multiple initial states
		// For now, we handle this is in a very inefficient way
		else {
			Expression init = modulesFile.getInitialStates();
			List<State> allPossStates = varList.getAllStates();
			for (State possState : allPossStates) {
				if (init.evaluateBoolean(modulesFile.getConstantValues(), possState)) {
					initStates.add(possState);
				}
			}
		}
		return initStates;
	}

	@Override
	public void exploreState(State exploreState) throws PrismException
	{
		this.exploreState = exploreState;
		transitionListBuilt = false;
	}
	
	@Override
	public State getExploreState()
	{
		return exploreState;
	}
	
	@Override
	public int getNumChoices() throws PrismException
	{
		return getTransitionList().getNumChoices();
	}

	@Override
	public int getNumTransitions() throws PrismException
	{
		return getTransitionList().getNumTransitions();
	}

	@Override
	public int getNumTransitions(int index) throws PrismException
	{
		return getTransitionList().getChoice(index).size();
	}

	@Override
	public String getTransitionAction(int index) throws PrismException
	{
		int a = getTransitionList().getTransitionModuleOrActionIndex(index);
		return a < 0 ? null : modulesFile.getSynch(a - 1);
	}

	@Override
	public String getTransitionAction(int index, int offset) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		int a = transitions.getTransitionModuleOrActionIndex(transitions.getTotalIndexOfTransition(index, offset));
		return a < 0 ? null : modulesFile.getSynch(a - 1);
	}

	@Override
	public String getChoiceAction(int index) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		int a = transitions.getChoiceModuleOrActionIndex(index);
		return a < 0 ? null : modulesFile.getSynch(a - 1);
	}

	@Override
	public double getTransitionProbability(int index, int offset) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		return transitions.getChoice(index).getProbability(offset);
	}

	//@Override
	public double getTransitionProbability(int index) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		return transitions.getTransitionProbability(index);
	}

	@Override
	public State computeTransitionTarget(int index, int offset) throws PrismException
	{
		return getTransitionList().getChoice(index).computeTarget(offset, exploreState);
	}

	//@Override
	public State computeTransitionTarget(int index) throws PrismException
	{
		return getTransitionList().computeTransitionTarget(index, exploreState);
	}
	
	@Override
	public boolean isLabelTrue(int i) throws PrismException
	{
		Expression expr = labelList.getLabel(i);
		return expr.evaluateBoolean(exploreState);
	}
	
	@Override
	public double getStateReward(int r, State state) throws PrismException
	{
		RewardStruct rewStr = modulesFile.getRewardStruct(r);
		int n = rewStr.getNumItems();
		double d = 0;
		for (int i = 0; i < n; i++) {
			if (!rewStr.getRewardStructItem(i).isTransitionReward()) {
				Expression guard = rewStr.getStates(i);
				if (guard.evaluateBoolean(modulesFile.getConstantValues(), state)) {
					double rew = rewStr.getReward(i).evaluateDouble(modulesFile.getConstantValues(), state);
					if (Double.isNaN(rew))
						throw new PrismLangException("Reward structure evaluates to NaN at state " + state, rewStr.getReward(i));
					d += rew;
				}
			}
		}
		return d;
	}

	@Override
	public double getStateActionReward(int r, State state, Object action) throws PrismException
	{
		RewardStruct rewStr = modulesFile.getRewardStruct(r);
		int n = rewStr.getNumItems();
		double d = 0;
		for (int i = 0; i < n; i++) {
			if (rewStr.getRewardStructItem(i).isTransitionReward()) {
				Expression guard = rewStr.getStates(i);
				String cmdAction = rewStr.getSynch(i);
				if (action == null ? (cmdAction.isEmpty()) : action.equals(cmdAction)) {
					if (guard.evaluateBoolean(modulesFile.getConstantValues(), state)) {
						double rew = rewStr.getReward(i).evaluateDouble(modulesFile.getConstantValues(), state);
						if (Double.isNaN(rew))
							throw new PrismLangException("Reward structure evaluates to NaN at state " + state, rewStr.getReward(i));
						d += rew;
					}
				}
			}
		}
		return d;
	}
	
	//@Override
	public void calculateStateRewards(State state, double[] store) throws PrismLangException
	{
		updater.calculateStateRewards(state, store);
	}
	
	@Override
	public VarList createVarList()
	{
		return varList;
	}
	
	// Miscellaneous (unused?) methods
	
	//@Override
	public void getRandomInitialState(RandomNumberGenerator rng, State initialState) throws PrismException
	{
		if (modulesFile.getInitialStates() == null) {
			initialState.copy(modulesFile.getDefaultInitialState());
		} else {
			throw new PrismException("Random choice of multiple initial states not yet supported");
		}
	}

	// Local utility methods
	
	/**
	 * Returns the current list of available transitions, generating it first if this has not yet been done.
	 */
	private TransitionList getTransitionList() throws PrismException
	{
		// Compute the current transition list, if required
		if (!transitionListBuilt) {
			updater.calculateTransitions(exploreState, transitionList);
			transitionListBuilt = true;
		}
		return transitionList;
	}

	@Override
	public boolean rewardStructHasTransitionRewards(int i)
	{
		return modulesFile.rewardStructHasTransitionRewards(i);
	}
	
	// GSMP methods
	
	public String getEventIdentOfChoice(int from, int to) {
		return ((ChoiceListFlexi)transitionList.getChoice(from)).getEventIdent(to);
	}
	
	/**
	 * 1) Transforms the modulesFile so that it becomes suitable for GSMP construction.
	 *    For example, all CTMC commands are translated into equivalent GSMP events.
	 * 2) Then various semantic checks are performed to ensure the would-be 
	 *    constructed GSMP complies with all the rules.
	 * 3) Lastly, constructs a list of GSMP events that can be assigned to GSMP models.
	 * @return Map of all GSMP events onto their unique identifiers
	 */
	public Map<String, GSMPEvent> setupGSMP() throws PrismException{
		translateCTMCCommandsIntoGSMPCommands();
		return getGSMPEvents();
	}
	
	/**
	 * Traverses the commands of each module and replace CTMC commands with GSMP event commands.
	 * This method is intended to be called at the beginning of building a GSMP.
	 * @throws PrismLangException Should never happen anyway. Necessary because this method needs to evaluate some expressions.
	 */
	private void translateCTMCCommandsIntoGSMPCommands() throws PrismLangException{
		Values constantList = modulesFile.getConstantList().evaluateSomeConstants(null, null);
		//traverse all modules and get all commands
		for (int i = 0; i < modulesFile.getNumModules() ; ++i) {
			for (int j = 0; j < modulesFile.getModule(i).getNumCommands() ; ++j) {
				Command comm = modulesFile.getModule(i).getCommand(j);
				//if it is already a valid GSMP command, just move on
				if (comm.isGSMPCommand()) {
					continue;
				}
				//create a new exponential distribution,
				ExpressionIdent distrIdent = new ExpressionIdent(
						"[autogenDistr_" + comm + "]");
				double rate = 0.0;
				//compute its rate - the sum of rates of updates
				for (int k = 0 ; k < comm.getUpdates().getNumUpdates() ; ++k) {
					Expression updateRate = comm.getUpdates().getProbability(k);
					if (updateRate == null) {
						rate += 1.0; //not sure if this should ever happen though
					} else {
						rate += updateRate.evaluateDouble(constantList);
					}
				}
				Expression rateExpr = new ExpressionLiteral(TypeDouble.getInstance(), rate);
				modulesFile.getDistributionList().addDistribution(
						distrIdent,
						rateExpr,
						null,
						TypeDistributionExponential.getInstance());
				//create a new astEvent using the new exponential distribution and assign it to the command
				ExpressionIdent eventIdent = new ExpressionIdent(
						"<CTMCevent_" + comm + ">");
				Event astEvent = new Event(eventIdent, distrIdent);
				modulesFile.getModule(i).addEvent(astEvent);
				comm.setEventIdent(eventIdent);
				//turn the rates of each update into probabilities
				for (int k = 0 ; k < comm.getUpdates().getNumUpdates() ; ++k) {
					Expression updateRate = comm.getUpdates().getProbability(k);
					double prob;
					if (updateRate == null) {
						prob = 1.0 / rate; // TODO MAJO - improve precision. This is also a good argument on why not to do it this way. Instead, we can have one event for each update and probability 1.
					} else {
						prob = updateRate.evaluateDouble(constantList) / rate;
					}
					Expression probExpr = new ExpressionLiteral(TypeDouble.getInstance(), prob);
					comm.getUpdates().setProbability(k, probExpr);
				}
			}
		}
	}
	
	/**
	 * Assumes that all CTMC commands have been translated into events via
	 * translateCTMCTransitionsIntoGSMPEvents() already!
	 * Otherwise, the CTMC commands are not included.
	 * @return list of all GSMP events excluding the product events
	 * @throws PrismLangException The distribution parameters could not be evaluated. This should never happen at this point.
	 */
	private Map<String, GSMPEvent> getGSMPEvents() throws PrismLangException{
		Map<String, GSMPEvent> events = new HashMap<String, GSMPEvent>();
		// traverse all modules and get all the ASTevents
		for (int i = 0; i < modulesFile.getNumModules() ; ++i) {
			for (int j = 0 ; j < modulesFile.getModule(i).getNumEvents() ; ++j) {
				// turn the ASTevent into a GSMPEvent and put it in the map
				GSMPEvent ev = generateGSMPEvent(modulesFile.getModule(i).getEvent(j));
				events.put(ev.getIdentifier(), ev);
			}
		}
		updater.setAllGSMPEvents(events);
		return events;
	}
	
	/**
	 * Assumes that methods setupGSMP() and then updater.calculateTransitions()
	 * have already been called!
	 * @return Complete, final list of all GMSP events, including translate CTMC events and GSMP event products
	 */
	public Map<String, GSMPEvent> getAllGSMPEventsAfterProducts(){
		return updater.getAllGSMPEvents();
	}
	
	/**
	 * Creates a new GSMP event from an astEvent of a modulesFile.
	 * @param astEvent
	 * @return GSMPEvent
	 * @throws PrismLangException The distribution parameters could not be evaluated. This should never happen at this point.
	 */
	private GSMPEvent generateGSMPEvent(Event astEvent) throws PrismLangException {
		//find the distribution assigned to the astEvent;
		DistributionList distributions = astEvent.getParent().getParent().getDistributionList();
		int distrIndex = distributions.getDistributionIndex(astEvent.getDistributionName());
		//obtain the distribution parameters
		TypeDistribution distributionType = distributions.getDistributionType(distrIndex);
		double firstParameter = 0;
		if (distributionType.getNumParams() >= 1) {
			firstParameter = distributions.getFirstParameter(distrIndex).evaluateDouble(distributions.getParent().getConstantValues());
		}
		double secondParameter = 0;
		if (distributionType.getNumParams() >= 2) {
			secondParameter = distributions.getSecondParameter(distrIndex).evaluateDouble(distributions.getParent().getConstantValues());
		}
		return (new GSMPEvent(distributionType, firstParameter, secondParameter, astEvent.getEventName()));
	}
	
}
