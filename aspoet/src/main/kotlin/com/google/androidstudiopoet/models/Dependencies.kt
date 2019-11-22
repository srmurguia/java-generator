/*
Copyright 2017 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.google.androidstudiopoet.models

open class ModuleDependency(override val name: String, val methodToCall: MethodToCall, override val method: String) : Dependency(name, method)

class AndroidModuleDependency(name: String, methodToCall: MethodToCall, method: String, val resourcesToRefer: ResourcesToRefer)
    : ModuleDependency(name, methodToCall, method)

data class LibraryDependency(override val method: String, override val name: String) : Dependency(name, method)

data class GmavenBazelDependency(override val name: String) : Dependency(name, null)

/**
 * Sort by class (Android -> Module -> Library), method, name
 */
abstract class Dependency(open val name: String, open val method: String?): Comparable<Dependency> {
  override fun compareTo(other: Dependency): Int {
    val diffClass = classToOrder(this) - classToOrder(other)
    if (diffClass != 0) {
      return diffClass
    }
    val otherMethod = other.method
    val myMethod = method
    if (myMethod == null) {
      if (otherMethod == null) {
        return 0
      }
      return -1
    }
    if (otherMethod == null) {
      return 1
    }
    val diffMethod = myMethod.compareTo(otherMethod)
    if (diffMethod != 0) {
      return diffMethod
    }
    return name.compareTo(other.name)
  }

  private fun classToOrder(other: Dependency) =
      when (other::class) {
        AndroidModuleDependency::class -> 0
        ModuleDependency::class -> 10
        LibraryDependency::class -> 20
        else -> 100
    }
}
