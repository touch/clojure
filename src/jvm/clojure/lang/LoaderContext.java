/**
 *   Copyright (c) Danny Wilson. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

/* blue May 4, 2014 */

package clojure.lang;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;

final public class LoaderContext
{
	final public static LoaderContext ROOT = new LoaderContext();

	static public LoaderContext get() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		return cl instanceof DynamicClassLoader? ((DynamicClassLoader) cl).context : ROOT;
	}

	final public HashMap<Integer, Object[]> constantVals = new HashMap<Integer, Object[]>();
	final public ConcurrentHashMap<Symbol, Namespace> namespaces = new ConcurrentHashMap<Symbol, Namespace>();

	final public ConcurrentHashMap<String, Reference<Class>> classCache = new ConcurrentHashMap<String, Reference<Class>>();
	final public ReferenceQueue classCacheReferenceQueue = new ReferenceQueue();

	public LoaderContext()
	{
		if (RT.CLOJURE_NS != null) {
			Symbol loadedLibs = Symbol.intern("*loaded-libs*");
			Var loadedLibsVar = (Var) RT.CLOJURE_NS.getMappings().valAt(loadedLibs);
			if (loadedLibsVar != null){
				ConcurrentHashMap<Symbol, Namespace> root = ROOT.namespaces;
				Namespace copy;
				// Inject the namespaces created by 'core.clj' and friends:
				copy = root.get(Symbol.intern("clojure.core.protocols")); this.namespaces.put(copy.name, copy);
				copy = root.get(Symbol.intern("clojure.instant"));        this.namespaces.put(copy.name, copy);
				copy = root.get(Symbol.intern("clojure.uuid"));           this.namespaces.put(copy.name, copy);
				copy = root.get(Symbol.intern("clojure.string"));         this.namespaces.put(copy.name, copy);
				copy = root.get(Symbol.intern("clojure.java.io"));        this.namespaces.put(copy.name, copy);
			}
		}
	}

	public IPersistentMap injectNamespaces(LoaderContext from, String nameRegex)
	{
		IPersistentMap injected = RT.map();
		if (from != this) for (Map.Entry<Symbol, Namespace> ns : from.namespaces.entrySet()) if (ns.getKey().name.matches(nameRegex)) {
			/* Breaks:
			Namespace newns = new Namespace(ns.getKey());
			newns.mappings.set(ns.getValue().mappings.get());
			newns.aliases .set(ns.getValue().aliases .get());
			myNamespaces.put(ns.getKey(), newns);
			*/
			this.namespaces.put(ns.getKey(), ns.getValue());
			injected = injected.assoc(ns.getKey(), ns.getValue());
		}
		return injected;
	}
}
