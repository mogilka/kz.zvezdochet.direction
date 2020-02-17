package kz.zvezdochet.direction.part;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.provider.DictionaryLabelProvider;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.ui.view.ModelListView;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.direction.provider.TransitLabelProvider;
import kz.zvezdochet.part.CosmogramComposite;
import kz.zvezdochet.part.ICalculable;
import kz.zvezdochet.service.AspectTypeService;
import kz.zvezdochet.service.HouseService;
import kz.zvezdochet.service.PlanetService;

/**
 * Поиск дирекций на указанный возраст
 * @author Natalie Didenko
 */
public class AgePart extends ModelListView implements ICalculable {
	private Spinner spFrom;
	private Spinner spTo;
	private ComboViewer cvPlanet;
	private ComboViewer cvHouse;
	private ComboViewer cvAspect;
	private Button btHouse;

	@Inject
	public AgePart() {}

	@PostConstruct @Override
	public View create(Composite parent) {
		return super.create(parent);
	}
	
	@Override
	public void initFilter(Composite parent) {
		grFilter = new Group(parent, SWT.NONE);
		grFilter.setText("Поиск");

		Label lb = new Label(grFilter, SWT.NONE);
		lb.setText("Возраст");
		spFrom = new Spinner(grFilter, SWT.BORDER);
		spFrom.setMinimum(0);
		spFrom.setMaximum(150);

		spTo = new Spinner(grFilter, SWT.BORDER);
		spTo.setMinimum(0);
		spTo.setMaximum(150);

		lb = new Label(grFilter, SWT.NONE);
		lb.setText("Сфера жизни");
		cvPlanet = new ComboViewer(grFilter, SWT.READ_ONLY | SWT.BORDER);
		cvHouse = new ComboViewer(grFilter, SWT.READ_ONLY | SWT.BORDER);

		lb = new Label(grFilter, SWT.NONE);
		lb.setText("Аспекты");
		cvAspect = new ComboViewer(grFilter, SWT.READ_ONLY | SWT.BORDER);

		btHouse = new Button(grFilter, SWT.BORDER | SWT.CHECK);
		lb = new Label(grFilter, SWT.NONE);
		lb.setText("Дирекции домов");

		GridLayoutFactory.swtDefaults().numColumns(10).applyTo(grFilter);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(grFilter);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(spFrom);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(spTo);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(cvPlanet.getCombo());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(cvHouse.getCombo());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(cvAspect.getCombo());
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).
			grab(false, false).applyTo(btHouse);
	}

	@Override
	protected String[] initTableColumns() {
		return EventPart.getTableColumns();
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new TransitLabelProvider();
	}

	private Event event;

	/**
	 * Поиск персоны, для которой делается прогноз
	 * @return персона
	 */
	public Event getEvent() {
		return event;
	}

	/**
	 * Инициализация персоны, для которой делается прогноз
	 * @param event персона
	 */
	public void setEvent(Event event) {
		this.event = event;
		int age = spFrom.getSelection();
		if (age < 1)
			spFrom.setSelection(0);
		age = spTo.getSelection();
		if (age < 1)
			spTo.setSelection(event.getAge());
	}

	@Override
	protected void initControls() {
		try {
			super.initControls();
			cvPlanet.setContentProvider(new ArrayContentProvider());
			cvPlanet.setLabelProvider(new DictionaryLabelProvider());
			List<Model> list = new PlanetService().getList();
			Planet planet = new Planet();
			planet.setId(0L);
			list.add(0, planet);
			cvPlanet.setInput(list);

			cvHouse.setContentProvider(new ArrayContentProvider());
			cvHouse.setLabelProvider(new DictionaryLabelProvider());
			list = new HouseService().getList();
			House house = new House();
			house.setId(0L);
			list.add(0, house);
			cvHouse.setInput(list);

			cvAspect.setContentProvider(new ArrayContentProvider());
			cvAspect.setLabelProvider(new DictionaryLabelProvider());
			list = new AspectTypeService().getList();
			AspectType aspect = new AspectType();
			aspect.setId(0L);
			list.add(0, aspect);
			cvAspect.setInput(list);
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean check(int mode) {
		if (spFrom.getSelection() > spTo.getSelection()) {
			DialogUtil.alertError("Укажите правильный период жизни");
			return false;
		} else if (null == event) {
			DialogUtil.alertError("Событие не задано");
			return false;
		}
		return true;
	}

	/**
	 * Возвращает выбранный начальный возраст
	 * @return начальный возраст
	 */
	public int getInitialAge() {
		return spFrom.getSelection();
	}

	/**
	 * Возвращает выбранный конечный возраст
	 * @return конечный возраст
	 */
	public int getFinalAge() {
		return spTo.getSelection();
	}

	/**
	 * Возвращает выбранную планету как сферу жизни
	 * @return планета
	 */
	public Planet getPlanet() {
		IStructuredSelection selection = (IStructuredSelection)cvPlanet.getSelection();
		if (selection.getFirstElement() != null) {
			Planet planet = (Planet)selection.getFirstElement();
			if (planet.getId() > 0)
				return planet;
		}
		return null;
	}

	/**
	 * Возвращает выбранный дом как сферу жизни
	 * @return астрологический дом
	 */
	public House getHouse() {
		IStructuredSelection selection = (IStructuredSelection)cvHouse.getSelection();
		if (selection.getFirstElement() != null) {
			House house = (House)selection.getFirstElement();
			if (house.getId() > 0)
				return house;
		}
		return null;
	}

	/**
	 * Возвращает выбранный тип аспекта
	 * @return тип аспекта
	 */
	public AspectType getAspect() {
		IStructuredSelection selection = (IStructuredSelection)cvAspect.getSelection();
		if (selection.getFirstElement() != null) {
			AspectType type = (AspectType)selection.getFirstElement();
			if (type.getId() > 0)
				return type;
		}
		return null;
	}

	/**
	 * Проверяет, считаем ли дирекции от домов
	 * @return true|false
	 */
	public boolean useHouse() {
		return btHouse.getSelection();
	}

	@Override
	public Model createModel() {
		return null;
	}

	@Override
	protected void initGroup() {
		cmpCosmogram = new CosmogramComposite(group, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(group);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(group);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).
			hint(514, 514).span(3, 1).grab(true, false).applyTo(cmpCosmogram);
	}

	private CosmogramComposite cmpCosmogram;

	@SuppressWarnings("unchecked")
	@Override
	public void onCalc(Object mode) {
		int initage = getInitialAge();
		int finage = getFinalAge();
		if (initage != finage)
			return;

		Event direvent = new Event();
		direvent.setRecalculable(false);
		direvent.init(false);
		direvent.setHouses(null);
		direvent.setAspectList((List<SkyPointAspect>)data);

		Collection<Planet> planets = event.getPlanets().values();
		Map<Long, Planet> planets2 = direvent.getPlanets();
		for (Planet planet : planets) {
			double coord = CalcUtil.incrementCoord(planet.getLongitude(), initage, true);
			planets2.get(planet.getId()).setLongitude(coord);
		}

		Map<String, Object> params = new HashMap<>();
		params.put("exact", true);
		params.put("houseAspectable", true);

		String[] atcodes = {"NEUTRAL", "POSITIVE", "NEGATIVE"};
		List<String> aparams = new ArrayList<String>();
		aparams.addAll(Arrays.asList(atcodes));
		params.put("aspects", aparams);

		cmpCosmogram.paint(event, direvent, params);
	}
}
