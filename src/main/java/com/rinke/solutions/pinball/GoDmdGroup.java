package com.rinke.solutions.pinball;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.beanutils.BeanUtils;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationModel;
import com.rinke.solutions.pinball.util.ApplicationProperties;

@Slf4j
public class GoDmdGroup {
	
	
	public GoDmdGroup(Composite parent) {
		grpGoDMDCrtls = new Group(parent, SWT.NONE);
		grpGoDMDCrtls.setLayout(new GridLayout(4, false));
		GridData gd_grpTest = new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1);
		grpGoDMDCrtls.setLayoutData(gd_grpTest);
		grpGoDMDCrtls.setText("goDMD Animation");
		grpGoDMDCrtls.setVisible(ApplicationProperties.getBoolean(ApplicationProperties.GODMD_ENABLED_PROP_KEY, false));


		Label lblCycle = new Label(grpGoDMDCrtls, SWT.NONE);
		lblCycle.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblCycle.setText("Cycle:");

		spinner = new Spinner(grpGoDMDCrtls, SWT.BORDER);
		GridData gd_spinner = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_spinner.widthHint = 53;
		spinner.setLayoutData(gd_spinner);

		Label lblHold = new Label(grpGoDMDCrtls, SWT.NONE);
		lblHold.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblHold.setText("Hold:");

		holdCycleText = new Text(grpGoDMDCrtls, SWT.BORDER);
		GridData gd_text_1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_text_1.widthHint = 52;
		holdCycleText.setLayoutData(gd_text_1);

		Label lblFsk = new Label(grpGoDMDCrtls, SWT.NONE);
		lblFsk.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblFsk.setText("Fsk:");

		fskCombo = new Combo(grpGoDMDCrtls, SWT.NONE);
		GridData gd_fskCombo = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_fskCombo.widthHint = 70;
		fskCombo.setLayoutData(gd_fskCombo);
		fskCombo.setItems(Arrays.stream(AnimationModel.Fsk.values()).map(f -> String.valueOf(f.n)).toArray(String[]::new));
		new Label(grpGoDMDCrtls, SWT.NONE);
		new Label(grpGoDMDCrtls, SWT.NONE);

		Label lblTransiton = new Label(grpGoDMDCrtls, SWT.NONE);
		lblTransiton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblTransiton.setText("Transition:");

		transitionCombo = new Combo(grpGoDMDCrtls, SWT.NONE);
		GridData gd_combo_4 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_combo_4.widthHint = 70;
		transitionCombo.setLayoutData(gd_combo_4);
		transitionCombo.setItems(new String[]{" - ", "generic"});
		
		transitionFrom = new Text(grpGoDMDCrtls, SWT.BORDER);
		transitionFrom.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		new Label(grpGoDMDCrtls, SWT.NONE);

		Label lblClockfrom = new Label(grpGoDMDCrtls, SWT.NONE);
		lblClockfrom.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblClockfrom.setText("ClockFrom:");

		clockFromText = new Text(grpGoDMDCrtls, SWT.BORDER);
		GridData gd_text_2 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_text_2.widthHint = 66;
		clockFromText.setLayoutData(gd_text_2);

		Button btnSet = new Button(grpGoDMDCrtls, SWT.NONE);
		btnSet.setText("Set");

		btnSmall = new Button(grpGoDMDCrtls, SWT.CHECK);
		btnSmall.setText("small");

		Label lblClockpos = new Label(grpGoDMDCrtls, SWT.NONE);
		lblClockpos.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblClockpos.setText("ClockPos:");
		
		clockPosX = new Text(grpGoDMDCrtls, SWT.BORDER);
		clockPosX.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		clockPosY = new Text(grpGoDMDCrtls, SWT.BORDER);
		clockPosY.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		btnFront = new Button(grpGoDMDCrtls, SWT.CHECK);
		btnFront.setText("front");

		initDataBindings();
	}
	
	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue observeTextClockFromTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(clockFromText);
		IObservableValue clockFromAniModelObserveValue = BeanProperties.value("clockFrom").observe(aniModel);
		bindingContext.bindValue(observeTextClockFromTextObserveWidget, clockFromAniModelObserveValue, null, null);
		//
		IObservableValue observeSelectionSpinnerObserveWidget = WidgetProperties.selection().observe(spinner);
		IObservableValue cyclesAniModelObserveValue = BeanProperties.value("cycles").observe(aniModel);
		bindingContext.bindValue(observeSelectionSpinnerObserveWidget, cyclesAniModelObserveValue, null, null);
		//
		IObservableValue observeTextHoldCycleTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(holdCycleText);
		IObservableValue holdCyclesAniModelObserveValue = BeanProperties.value("holdCycles").observe(aniModel);
		bindingContext.bindValue(observeTextHoldCycleTextObserveWidget, holdCyclesAniModelObserveValue, null, null);
		//
		IObservableValue observeSelectionFskComboObserveWidget = WidgetProperties.selection().observe(fskCombo);
		IObservableValue fskAniModelObserveValue = BeanProperties.value("fsk").observe(aniModel);
		bindingContext.bindValue(observeSelectionFskComboObserveWidget, fskAniModelObserveValue, null, null);
		//
		IObservableValue observeTextClockPosXObserveWidget = WidgetProperties.text(SWT.Modify).observe(clockPosX);
		IObservableValue clockXOffsetAniModelObserveValue = BeanProperties.value("clockXOffset").observe(aniModel);
		bindingContext.bindValue(observeTextClockPosXObserveWidget, clockXOffsetAniModelObserveValue, null, null);
		//
		IObservableValue observeTextClockPosYObserveWidget = WidgetProperties.text(SWT.Modify).observe(clockPosY);
		IObservableValue clockYOffsetAniModelObserveValue = BeanProperties.value("clockYOffset").observe(aniModel);
		bindingContext.bindValue(observeTextClockPosYObserveWidget, clockYOffsetAniModelObserveValue, null, null);
		//
		IObservableValue observeSelectionBtnFrontObserveWidget = WidgetProperties.selection().observe(btnFront);
		IObservableValue clockInFrontAniModelObserveValue = BeanProperties.value("clockInFront").observe(aniModel);
		bindingContext.bindValue(observeSelectionBtnFrontObserveWidget, clockInFrontAniModelObserveValue, null, null);
		//
		IObservableValue observeSelectionBtnSmallObserveWidget = WidgetProperties.selection().observe(btnSmall);
		IObservableValue clockSmallAniModelObserveValue = BeanProperties.value("clockSmall").observe(aniModel);
		bindingContext.bindValue(observeSelectionBtnSmallObserveWidget, clockSmallAniModelObserveValue, null, null);
		//
		IObservableValue observeTextTransitionFromObserveWidget = WidgetProperties.text(SWT.Modify).observe(transitionFrom);
		IObservableValue transitionFromAniModelObserveValue = BeanProperties.value("transitionFrom").observe(aniModel);
		bindingContext.bindValue(observeTextTransitionFromObserveWidget, transitionFromAniModelObserveValue, new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER), null);
		//
		return bindingContext;
	}
	
	public void updateAnimation(Animation current) {
		try {
			BeanUtils.copyProperties(current, aniModel);
		} catch (Exception e1) {
			log.warn("mapping problems writing ani", e1);
		}
	}

	public void updateAniModel(Animation a) {
		try {
			BeanUtils.copyProperties(aniModel, a);
			log.info("ani hold: {}, fsk: {}, cycles: {}, from: {}", a.getHoldCycles(), a.getFsk(), a.getCycles(), a.getClockFrom());
		} catch (Exception e1) {
			log.warn("mapping problems reading ani", e1);
		}

	}



	private Text holdCycleText;
	private Text clockFromText;
	private Spinner spinner;
	private Combo fskCombo;
	private Button btnSmall;

	AnimationModel aniModel = new AnimationModel();
	private Text clockPosX;
	private Text clockPosY;
	private Text transitionFrom;
	private Button btnFront;

	Combo transitionCombo;
	Group grpGoDMDCrtls;

}
