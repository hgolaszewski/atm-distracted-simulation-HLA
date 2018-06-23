package hla.atm.commons;

import static hla.atm.utils.Utils.convertTime;
import static hla.atm.utils.Utils.log;
import static hla.atm.config.Config.SYNC_POINT;

import java.util.ArrayList;

import hla.atm.commons.event.ExternalEvent;
import hla.rti.LogicalTime;
import hla.rti.jlc.NullFederateAmbassador;

public abstract class AbstractAmbassador extends NullFederateAmbassador {

	private double federateLookahead = 1.0;
	private double federateTime  = 0.0;
	private double federateStep = 2.0;

	private boolean isConstrained = false;
	private boolean isRegulating = false;
	private boolean isAdvancing = false;

	private boolean isReadyToRun = false;
	private boolean isAnnounced = false;

	private boolean running = true;

	private String ambassadorName;

	private ArrayList<ExternalEvent> externalEvents = new ArrayList<>();

	public AbstractAmbassador() {}

	public AbstractAmbassador(String ambassadorName) {
		this.ambassadorName = ambassadorName;
	}

	public void synchronizationPointRegistrationFailed(String label) {
		log(ambassadorName + ": Failed to register sync point: " + label);
	}

	public void synchronizationPointRegistrationSucceeded(String label) {
		log(ambassadorName + ": Successfully registered sync point: " + label);
	}

	public void announceSynchronizationPoint(String label, byte[] tag) {
		log(ambassadorName + ": synchronization point announced: " + label);
		if(label.equals(SYNC_POINT)) {
			this.isAnnounced = true;
		}
	}

	public void federationSynchronized(String label) {
		log(ambassadorName + ": federation Synchronized: " + label);
		if(label.equals(SYNC_POINT)) {
			this.isReadyToRun = true;
		}
	}

	public void timeRegulationEnabled(LogicalTime theFederateTime) {
		this.federateTime = convertTime(theFederateTime);
		this.isRegulating = true;
	}

	public void timeConstrainedEnabled(LogicalTime theFederateTime) {
		this.federateTime = convertTime(theFederateTime);
		this.isConstrained = true;
	}

	public void timeAdvanceGrant(LogicalTime theTime) {
		this.federateTime = convertTime(theTime);
		this.isAdvancing = false;
	}

	// Getters & Setters

	public double getFederateTime() {
		return federateTime;
	}

	public void setFederateTime(double federateTime) {
		this.federateTime = federateTime;
	}

	public double getFederateLookahead() {
		return federateLookahead;
	}

	public void setFederateLookahead(double federateLookahead) {
		this.federateLookahead = federateLookahead;
	}

	public double getFederateStep() {
		return federateStep;
	}

	public void setFederateStep(double federateStep) {
		this.federateStep = federateStep;
	}

	public boolean isRegulating() {
		return isRegulating;
	}

	public void setRegulating(boolean regulating) {
		isRegulating = regulating;
	}

	public boolean isConstrained() {
		return isConstrained;
	}

	public void setConstrained(boolean constrained) {
		isConstrained = constrained;
	}

	public boolean isAdvancing() {
		return isAdvancing;
	}

	public void setAdvancing(boolean advancing) {
		isAdvancing = advancing;
	}

	public boolean isAnnounced() {
		return isAnnounced;
	}

	public void setAnnounced(boolean announced) {
		isAnnounced = announced;
	}

	public boolean isReadyToRun() {
		return isReadyToRun;
	}

	public void setReadyToRun(boolean readyToRun) {
		isReadyToRun = readyToRun;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	protected String getAmbassadorName() {
		return ambassadorName;
	}

	public void setAmbassadorName(String ambassadorName) {
		this.ambassadorName = ambassadorName;
	}

	public ArrayList<ExternalEvent> getExternalEvents() {
		return externalEvents;
	}

	public void setExternalEvents(ArrayList<ExternalEvent> externalEvents) {
		this.externalEvents = externalEvents;
	}

}
