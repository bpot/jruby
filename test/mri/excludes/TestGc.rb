exclude :test_count, "expects GC.start to force GC run"
exclude :test_exception_in_finalizer, "broken subprocess logic"
exclude :test_expand_heap, "broken subprocess logic"
exclude :test_finalizing_main_thread, "broken subprocess logic"
exclude :test_gc_parameter, "MRI-specific"
exclude :test_gc_internals, "MRI-specific"
exclude :test_latest_gc_info, ""
exclude :test_latest_gc_info_argument, ""
exclude :test_profiler_clear, "broken subprocess logic"
exclude :test_profiler_total_time, ""
exclude :test_singleton_method, "broken subprocess logic"
exclude :test_singleton_method_added, "broken subprocess logic"
exclude :test_start_full_mark, ""
exclude :test_start_immediate_sweep, ""
exclude :test_stat_single, ""
exclude :test_stat, "tests count_objects"
exclude :test_sweep_in_finalizer, ""
exclude :test_verify_internal_consistency, ""